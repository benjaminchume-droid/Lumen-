package com.lumen.reader.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

object CatalogService {

    /** Shared client: connection reuse + shorter timeouts for snappier catalogs. */
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(8, 2, TimeUnit.MINUTES))
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    private val UA =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    private fun httpGet(url: String, referer: String? = null): String? {
        return try {
            val b = Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .header("Accept", "text/html,application/xhtml+xml,application/json")
                .header("Accept-Language", "en-US,en;q=0.9")
            if (!referer.isNullOrBlank()) b.header("Referer", referer)
            client.newCall(b.build()).execute().use { resp ->
                if (!resp.isSuccessful) return null
                resp.body?.string()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun abs(base: String, href: String?): String? {
        if (href.isNullOrBlank() || href.startsWith("data:") || href.startsWith("javascript:")) return null
        if (href.startsWith("#")) return null
        if (href.startsWith("//")) return "https:$href"
        if (href.startsWith("http")) return href
        val root = try {
            val u = java.net.URI(base)
            "${u.scheme}://${u.host}" + if (u.port > 0 && u.port != 80 && u.port != 443) ":${u.port}" else ""
        } catch (_: Exception) {
            base.trimEnd('/').substringBefore("/manga").substringBefore("/novel").ifBlank { base.trimEnd('/') }
        }
        return if (href.startsWith("/")) "$root$href" else {
            val pathBase = base.substringBeforeLast('/').trimEnd('/')
            "$pathBase/$href"
        }
    }

    private fun imgSrc(el: Element?): String? {
        if (el == null) return null
        return sequenceOf(
            el.attr("data-src"),
            el.attr("data-lazy-src"),
            el.attr("data-original"),
            el.attr("data-srcset").substringBefore(" "),
            el.attr("srcset").substringBefore(" "),
            el.attr("src")
        ).firstOrNull {
            it.isNotBlank() && !it.contains("data:image/svg") && !it.contains("placeholder") &&
                !it.contains("grey.gif") && !it.contains("loading")
        }
    }

    data class LiveSeries(
        val id: String,
        val title: String,
        val url: String,
        val coverUrl: String?,
        val sourceName: String,
        val sourceId: String,
        val kind: String,
        val author: String = "",
        val description: String = "",
        val genres: List<String> = emptyList()
    )

    data class LiveChapter(
        val id: String,
        val title: String,
        val url: String,
        val number: Float
    )

    suspend fun fetchPopularFromInstalled(entries: List<IndexEntry>, perSource: Int = 120): List<LiveSeries> =
        withContext(Dispatchers.IO) {
            coroutineScope {
                entries.map { entry ->
                    async {
                        runCatching { fetchPopular(entry, perSource) }.getOrDefault(emptyList())
                    }
                }.awaitAll().flatten()
            }
        }

    fun fetchPopular(entry: IndexEntry, limit: Int = 40): List<LiveSeries> {
        val base = entry.site?.trim()?.trimEnd('/') ?: return emptyList()
        if (!base.startsWith("http")) return emptyList()

        val out = LinkedHashMap<String, LiveSeries>()
        val isNovel = entry.kind == MediaKind.NOVEL
        // Fewer URLs first → stop early when full (speed)
        val pageUrls = buildList {
            if (isNovel) {
                add("$base/novel/?m_orderby=views")
                add("$base/novel/")
                add("$base/most-popular")
                add("$base/latest-release-novel")
                for (p in 1..3) add("$base/novel/page/$p/?m_orderby=views")
            } else {
                add("$base/manga/?m_orderby=views")
                add("$base/manga/")
                for (p in 1..3) add("$base/manga/page/$p/?m_orderby=views")
            }
            add(base)
        }

        for (url in pageUrls.distinct()) {
            if (out.size >= limit) break
            val html = httpGet(url, base) ?: continue
            for (item in parseSeriesList(html, base, entry, limit - out.size)) {
                if (!out.containsKey(item.id)) out[item.id] = item
            }
        }
        return out.values.toList()
    }

    private fun parseSeriesList(html: String, base: String, entry: IndexEntry, limit: Int): List<LiveSeries> {
        val doc = Jsoup.parse(html, base)
        val out = LinkedHashMap<String, LiveSeries>()

        fun add(title: String, href: String, cover: String?) {
            val t = title.trim().replace(Regex("\\s+"), " ")
            if (t.length < 2) return
            val low = t.lowercase()
            val bad = listOf(
                "action novels", "drama novels", "romance novels", "adult novels",
                "see more", "latest release", "new novel", "completed", "genre",
                "home", "login", "register", "contact", "privacy", "most popular"
            )
            if (bad.any { low == it || (low.startsWith(it) && t.length < 28) }) return
            if (href.contains("/genre/") || href.contains("/tag/")) return
            if (href.endsWith("/novel/") || href.endsWith("/manga/")) return
            val id = "${entry.id}|$href"
            if (out.containsKey(id)) return
            out[id] = LiveSeries(
                id = id,
                title = t.take(140),
                url = href,
                coverUrl = cover,
                sourceName = entry.name,
                sourceId = entry.id,
                kind = if (entry.kind == MediaKind.NOVEL) "novel" else "manga"
            )
        }

        for (card in doc.select(".page-item-detail, .c-tabs-item__content, .row.c-tabs-item, .bs, .bsx, .manga-item, .novel-item")) {
            if (out.size >= limit) break
            val a = card.selectFirst(".post-title a, h3 a, h5 a, a") ?: continue
            val href = abs(base, a.attr("href")) ?: continue
            add(a.text().ifBlank { a.attr("title") }, href, abs(base, imgSrc(card.selectFirst("img"))))
        }

        for (a in doc.select("h3 a, .post-title a")) {
            if (out.size >= limit) break
            val href = abs(base, a.attr("href")) ?: continue
            if (!href.contains("/novel/") && !href.contains("/manga/") && !href.contains("/manhwa/") &&
                !href.contains("-novel.html")
            ) continue
            val coverEl = a.parents().firstOrNull { it.selectFirst("img") != null }?.selectFirst("img")
            add(a.text().ifBlank { a.attr("title") }, href, abs(base, imgSrc(coverEl)))
        }

        for (a in doc.select("a[href*=-novel.html], a[href*=/novel/], a[href*=/manga/]")) {
            if (out.size >= limit) break
            val href = abs(base, a.attr("href")) ?: continue
            if (href.contains("latest-release") || href.contains("new-novel") || href.contains("completed-novel")) continue
            val title = a.attr("title").ifBlank { a.text() }.trim()
            if (title.length < 4) continue
            val wrap = a.closest(".item, li, .col-truyen, .row, .bsx") ?: a.parent()
            add(title, href, abs(base, imgSrc(wrap?.selectFirst("img") ?: a.selectFirst("img"))))
        }

        return out.values.toList()
    }

    fun fetchDetailsAndChapters(seriesUrl: String, entry: IndexEntry): Pair<LiveSeries, List<LiveChapter>> {
        val html = httpGet(seriesUrl) ?: return LiveSeries(
            id = "${entry.id}|$seriesUrl", title = "Unknown", url = seriesUrl,
            coverUrl = null, sourceName = entry.name, sourceId = entry.id,
            kind = if (entry.kind == MediaKind.NOVEL) "novel" else "manga"
        ) to emptyList()

        val doc = Jsoup.parse(html, seriesUrl)
        val title = doc.selectFirst(
            "h1, .post-title h1, .manga-title, .entry-title, .truyen-title, .book-title, .novel-title, .series-title"
        )?.text()?.trim()?.ifBlank { null }
            ?: doc.title().substringBefore("|").substringBefore("-").trim()

        val cover = abs(
            seriesUrl,
            imgSrc(
                doc.selectFirst(
                    ".summary_image img, .thumb img, .manga-thumb img, .book-cover img, " +
                        ".novel-cover img, .series-cover img, .books .book img"
                )
            ) ?: doc.selectFirst("meta[property=og:image]")?.attr("content")
        )

        val desc = doc.selectFirst(
            ".description-summary, .summary__content, .manga-excerpt, .desc-text, " +
                ".novel-summary, #editdescription, meta[name=description]"
        )?.let { if (it.tagName() == "meta") it.attr("content") else it.text() }?.trim().orEmpty()

        val author = doc.selectFirst(
            ".author-content a, .artist-content a, .manga-author, .book-author, a[itemprop=author]"
        )?.text()?.trim().orEmpty()

        val genres = doc.select(".genres-content a, .mgen a, a[rel=tag], .tag-list a")
            .map { it.text().trim() }.filter { it.isNotBlank() }.take(10)

        // Always merge HTML + AJAX + paginated chapter lists (missing chapters fix)
        val byUrl = LinkedHashMap<String, LiveChapter>()
        fun merge(list: List<LiveChapter>) {
            for (c in list) {
                val existing = byUrl[c.url]
                if (existing == null || (c.number > 0 && existing.number <= 0)) {
                    byUrl[c.url] = c
                }
            }
        }
        merge(extractChapters(doc, seriesUrl))
        merge(fetchAjaxChapters(seriesUrl, doc))
        merge(fetchPaginatedChapters(seriesUrl, doc))

        val chapters = byUrl.values
            .map { normalizeChapterNumber(it) }
            .sortedByDescending { it.number }

        val series = LiveSeries(
            id = "${entry.id}|$seriesUrl",
            title = title.ifBlank { "Series" },
            url = seriesUrl,
            coverUrl = cover,
            sourceName = entry.name,
            sourceId = entry.id,
            kind = if (entry.kind == MediaKind.NOVEL) "novel" else "manga",
            author = author,
            description = desc.take(1200),
            genres = genres
        )
        return series to chapters
    }

    private fun normalizeChapterNumber(ch: LiveChapter): LiveChapter {
        if (ch.number > 0f) return ch
        val fromUrl = Regex(
            """(?:chapter|ch|episode|ep)[-_]?(\d+(?:\.\d+)?)""",
            RegexOption.IGNORE_CASE
        ).find(ch.url)?.groupValues?.get(1)?.toFloatOrNull()
        val fromName = Regex("""(\d+(?:\.\d+)?)""").find(ch.title)?.groupValues?.get(1)?.toFloatOrNull()
        val n = fromUrl ?: fromName ?: 0f
        return if (n > 0f) ch.copy(number = n) else ch
    }

    private fun extractChapters(doc: Document, seriesUrl: String): List<LiveChapter> {
        val chapters = LinkedHashMap<String, LiveChapter>()
        val selectors = listOf(
            "li.wp-manga-chapter a",
            ".listing-chapters_wrap a",
            "ul.mainversion-chap a",
            "ul.main li a",
            ".version-chap li a",
            ".chapter-list a",
            "#chapter-list a",
            "#list-chapter a",
            ".list-chapter a",
            "ul.list-chapter a",
            ".eplister a",
            "#chapterlist a",
            ".chapters a",
            ".row-chapter a",
            "ul.chapter-list a",
            ".manga-chapters a",
            "a.chapter-item",
            "a[href*=/chapter-], a[href*=/chapter/], a[href*=chapter-], a[href*=/ch-]"
        )
        var auto = 0f
        for (sel in selectors) {
            for (a in doc.select(sel)) {
                val href = abs(seriesUrl, a.attr("href")) ?: continue
                val path = href.lowercase()
                if (path.contains("/genre/") || path.contains("/tag/") || path.contains("/author/")) continue
                if (path.endsWith("/novel/") || path.endsWith("/manga/")) continue
                if (path.contains("/page/") && !path.contains("chapter")) continue
                var name = a.text().trim().ifBlank { a.attr("title") }.ifBlank {
                    a.attr("data-title")
                }.ifBlank { "Chapter" }.replace(Regex("\\s+"), " ")
                if (name.equals("prev", true) || name.equals("next", true) || name.equals("home", true)) continue
                if (name.length < 1) continue
                if (chapters.containsKey(href)) continue
                val numMatch = Regex("""(\d+(?:\.\d+)?)""").find(name)
                val num = numMatch?.groupValues?.get(1)?.toFloatOrNull() ?: (++auto)
                chapters[href] = LiveChapter(id = href, title = name.take(120), url = href, number = num)
            }
            if (chapters.size > 15000) break
        }
        // data attributes often hold full lists on Madara
        for (el in doc.select("[data-chapter], [data-chapter-url], li[data-num]")) {
            val href = abs(
                seriesUrl,
                el.attr("data-chapter-url").ifBlank { el.selectFirst("a")?.attr("href") }
            ) ?: continue
            if (chapters.containsKey(href)) continue
            val name = el.attr("data-chapter").ifBlank { el.text() }.ifBlank { "Chapter" }
            val num = el.attr("data-num").toFloatOrNull()
                ?: Regex("""(\d+(?:\.\d+)?)""").find(name)?.groupValues?.get(1)?.toFloatOrNull()
                ?: (++auto)
            chapters[href] = LiveChapter(id = href, title = name.take(120), url = href, number = num)
        }
        return chapters.values.toList()
    }

    /** Always attempt Madara-style AJAX chapter endpoints (full lists). */
    private fun fetchAjaxChapters(seriesUrl: String, doc: Document): List<LiveChapter> {
        val out = ArrayList<LiveChapter>()
        val base = seriesUrl.trimEnd('/')
        val root = try {
            val u = java.net.URI(seriesUrl)
            "${u.scheme}://${u.host}"
        } catch (_: Exception) {
            base.substringBefore("/manga").substringBefore("/novel").ifBlank { base }
        }

        val postId = sequenceOf(
            doc.selectFirst("#manga-chapters-holder")?.attr("data-id"),
            doc.selectFirst(".wp-manga-action-button")?.attr("data-post"),
            doc.selectFirst("[data-id][id*=chapter]")?.attr("data-id"),
            doc.selectFirst("input[name=manga_id]")?.attr("value"),
            Regex("""\"manga_id\"\\s*:\\s*(\d+)""").find(doc.html())?.groupValues?.get(1),
            Regex("""data-id=[\"'](\d+)[\"']""").find(doc.html())?.groupValues?.get(1)
        ).firstOrNull { !it.isNullOrBlank() }

        val candidates = mutableListOf(
            "$base/ajax/chapters/",
            "$base/ajax/chapters",
            "$root/wp-admin/admin-ajax.php"
        )

        for (url in candidates.distinct()) {
            val html = try {
                when {
                    url.contains("admin-ajax") && !postId.isNullOrBlank() -> {
                        val form = FormBody.Builder()
                            .add("action", "manga_get_chapters")
                            .add("manga", postId!!)
                            .build()
                        val req = Request.Builder().url(url)
                            .header("User-Agent", UA)
                            .header("X-Requested-With", "XMLHttpRequest")
                            .header("Referer", seriesUrl)
                            .header("Accept", "*/*")
                            .post(form).build()
                        client.newCall(req).execute().use { if (it.isSuccessful) it.body?.string() else null }
                    }
                    url.contains("ajax/chapters") -> {
                        val req = Request.Builder().url(url)
                            .header("User-Agent", UA)
                            .header("X-Requested-With", "XMLHttpRequest")
                            .header("Referer", seriesUrl)
                            .header("Accept", "*/*")
                            .post(ByteArray(0).toRequestBody(null)).build()
                        client.newCall(req).execute().use { if (it.isSuccessful) it.body?.string() else null }
                    }
                    else -> null
                }
            } catch (_: Exception) {
                null
            } ?: continue
            if (html.length < 40) continue
            out += extractChapters(Jsoup.parse(html, seriesUrl), seriesUrl)
            if (out.size > 50) break
        }
        return out
    }

    /** Follow chapter-list pagination links when present. */
    private fun fetchPaginatedChapters(seriesUrl: String, doc: Document): List<LiveChapter> {
        val out = ArrayList<LiveChapter>()
        val pageLinks = doc.select(
            ".pagination a, .nav-links a, a.page-numbers, .wp-pagenavi a, " +
                "a[href*=/page/][href*=chapter], a[href*=chapters/page]"
        ).mapNotNull { abs(seriesUrl, it.attr("href")) }
            .distinct()
            .take(8)
        for (url in pageLinks) {
            if (url == seriesUrl) continue
            val html = httpGet(url, seriesUrl) ?: continue
            out += extractChapters(Jsoup.parse(html, seriesUrl), seriesUrl)
            if (out.size > 5000) break
        }
        return out
    }

    fun fetchChapterPages(chapterUrl: String, isNovel: Boolean): List<String> {
        val html = httpGet(chapterUrl) ?: return emptyList()
        val doc = Jsoup.parse(html, chapterUrl)

        if (isNovel) {
            val candidates = listOf(
                ".chapter-c", "#chapter-c", "#chr-content", "#chapter-content",
                ".chapter-content", ".reading-content", ".text-left", ".chr-c",
                ".box-chap", ".novel-content", "#content", ".entry-content", "article .content"
            )
            var text = ""
            for (sel in candidates) {
                val el = doc.selectFirst(sel) ?: continue
                val clone = el.clone()
                clone.select("script, style, nav, .ads, .chapter-nav, select, option, button, form").remove()
                val t = clone.text().trim()
                if (t.length > 200) {
                    text = t
                    break
                }
            }
            if (text.length < 80) {
                text = doc.select("div, article, section")
                    .map { it.text().trim() }
                    .filter { it.length > 400 }
                    .maxByOrNull { it.length }
                    .orEmpty()
            }
            val junk = listOf(
                "Font Size", "Font Family", "Background Color", "Reset Chapter",
                "A Settings", "16px", "Translator:", "Editor:", "Prev Next", "Chapter list", "Settings"
            )
            for (j in junk) text = text.replace(j, " ")
            text = text.replace(Regex("\\s{2,}"), " ").trim()
            if (text.length > 80) {
                return text.split(Regex("(?<=[.!?…])\\s+"))
                    .chunked(5)
                    .map { it.joinToString(" ") }
                    .filter { it.length > 12 }
            }
            return listOf(text.ifBlank { "No text extracted." })
        }

        doc.select("script, style, nav, header, footer, .ads").remove()
        val imgs = doc.select(
            ".reading-content img, .page-break img, #readerarea img, .chapter-content img, " +
                ".entry-content img, img.wp-manga-chapter-img, #chapter-content img, .container-chapter-reader img"
        )
        val urls = imgs.mapNotNull { abs(chapterUrl, imgSrc(it)) }
            .filter { it.contains("http") && !it.contains("logo") && !it.contains("avatar") }
            .distinct()
        return urls.ifEmpty {
            doc.select("img").mapNotNull { abs(chapterUrl, imgSrc(it)) }
                .filter { it.length > 20 && it.contains("http") }.take(80)
        }
    }
}
