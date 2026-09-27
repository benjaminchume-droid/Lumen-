package com.lumen.reader.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

object CatalogService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private fun httpGet(url: String): String? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "en-US,en;q=0.9,ar;q=0.8")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                resp.body?.string()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun abs(base: String, href: String?): String? {
        if (href.isNullOrBlank() || href.startsWith("data:") || href.startsWith("javascript:")) return null
        if (href.startsWith("//")) return "https:$href"
        if (href.startsWith("http")) return href
        val root = base.trimEnd('/')
        return if (href.startsWith("/")) "$root$href" else "$root/$href"
    }

    private fun imgSrc(el: Element?): String? {
        if (el == null) return null
        return sequenceOf(
            el.attr("data-src"),
            el.attr("data-lazy-src"),
            el.attr("data-original"),
            el.attr("src")
        ).firstOrNull { it.isNotBlank() && !it.contains("data:image/svg") && !it.contains("placeholder") }
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

    fun fetchPopular(entry: IndexEntry, limit: Int = 120): List<LiveSeries> {
        val base = entry.site?.trim()?.trimEnd('/') ?: return emptyList()
        if (!base.startsWith("http")) return emptyList()

        val out = LinkedHashMap<String, LiveSeries>()
        val isNovel = entry.kind == MediaKind.NOVEL
        val pageUrls = buildList {
            if (isNovel) {
                add("$base/novel/")
                add("$base/novel/?m_orderby=views")
                add("$base/latest-release-novel")
                add("$base/most-popular")
                for (p in 1..8) {
                    add("$base/novel/page/$p/?m_orderby=views")
                    add("$base/novel/page/$p/")
                    add("$base/page/$p/")
                }
            } else {
                add("$base/manga/?m_orderby=views")
                add("$base/manga/")
                for (p in 1..8) {
                    add("$base/manga/page/$p/?m_orderby=views")
                    add("$base/manga/page/$p/")
                }
            }
            add("$base/?s=&post_type=wp-manga")
            add(base)
        }

        for (url in pageUrls.distinct()) {
            if (out.size >= limit) break
            val html = httpGet(url) ?: continue
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

        for (card in doc.select(".page-item-detail, .c-tabs-item__content, .row.c-tabs-item, .bs, .bsx")) {
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
            "h1, .post-title h1, .manga-title, .entry-title, .truyen-title, .book-title, .novel-title, .title"
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

        val chapters = extractChapters(doc, seriesUrl).toMutableList()
        if (chapters.size < 40) {
            val ajaxExtra = fetchAjaxChapters(seriesUrl, doc)
            val seen = chapters.map { it.url }.toMutableSet()
            for (c in ajaxExtra) {
                if (c.url !in seen) {
                    chapters.add(c)
                    seen.add(c.url)
                }
            }
        }

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
        return series to chapters.sortedByDescending { it.number }
    }

    private fun extractChapters(doc: Document, seriesUrl: String): List<LiveChapter> {
        val chapters = LinkedHashMap<String, LiveChapter>()
        val selectors = listOf(
            "li.wp-manga-chapter a",
            ".listing-chapters_wrap a",
            "ul.mainversion-chap a",
            "ul.main li a",
            ".chapter-list a",
            "#chapter-list a",
            "#list-chapter a",
            ".list-chapter a",
            "ul.list-chapter a",
            ".eplister a",
            "#chapterlist a",
            "a[href*=/chapter-], a[href*=/chapter/], a[href*=chapter-]"
        )
        var auto = 0f
        for (sel in selectors) {
            for (a in doc.select(sel)) {
                val href = abs(seriesUrl, a.attr("href")) ?: continue
                val path = href.lowercase()
                if (path.contains("/genre/") || path.contains("/tag/")) continue
                if (path.endsWith("/novel/") || path.endsWith("/manga/")) continue
                var name = a.text().trim().ifBlank { a.attr("title") }.ifBlank { "Chapter" }
                    .replace(Regex("\\s+"), " ")
                if (name.equals("prev", true) || name.equals("next", true) || name.equals("home", true)) continue
                if (name.length < 1) continue
                if (chapters.containsKey(href)) continue
                val numMatch = Regex("""(\\d+(?:\\.\\d+)?)""").find(name)
                val num = numMatch?.groupValues?.get(1)?.toFloatOrNull() ?: (++auto)
                chapters[href] = LiveChapter(id = href, title = name.take(120), url = href, number = num)
            }
            if (chapters.size > 8000) break
        }
        if (chapters.size < 5) {
            for (a in doc.select("a[href*=chapter-]")) {
                val href = abs(seriesUrl, a.attr("href")) ?: continue
                if (chapters.containsKey(href)) continue
                val name = a.text().trim().ifBlank {
                    href.substringAfterLast('/').removeSuffix(".html").replace('-', ' ')
                }
                val numMatch = Regex("""chapter-(\\d+)""", RegexOption.IGNORE_CASE).find(href)
                val num = numMatch?.groupValues?.get(1)?.toFloatOrNull() ?: (++auto)
                chapters[href] = LiveChapter(id = href, title = name.take(120), url = href, number = num)
            }
        }
        return chapters.values.sortedByDescending { it.number }
    }

    private fun fetchAjaxChapters(seriesUrl: String, doc: Document): List<LiveChapter> {
        val out = ArrayList<LiveChapter>()
        val base = seriesUrl.trimEnd('/')
        val candidates = mutableListOf("$base/ajax/chapters/", "$base/ajax/chapters")
        val postId = doc.selectFirst("#manga-chapters-holder")?.attr("data-id")
            ?: doc.selectFirst("[data-id]")?.attr("data-id")
        if (!postId.isNullOrBlank()) {
            val root = base.substringBefore("/manga").substringBefore("/novel").ifBlank { base }
            candidates += "$root/wp-admin/admin-ajax.php"
        }
        for (url in candidates.distinct()) {
            val html = try {
                if (url.contains("admin-ajax") && !postId.isNullOrBlank()) {
                    val form = FormBody.Builder()
                        .add("action", "manga_get_chapters")
                        .add("manga", postId)
                        .build()
                    val req = Request.Builder().url(url)
                        .header("User-Agent", "Mozilla/5.0")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("Referer", seriesUrl)
                        .post(form).build()
                    client.newCall(req).execute().use { if (it.isSuccessful) it.body?.string() else null }
                } else {
                    val req = Request.Builder().url(url)
                        .header("User-Agent", "Mozilla/5.0")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("Referer", seriesUrl)
                        .post(ByteArray(0).toRequestBody(null)).build()
                    client.newCall(req).execute().use { if (it.isSuccessful) it.body?.string() else null }
                }
            } catch (_: Exception) { null } ?: continue
            if (html.length < 40) continue
            out += extractChapters(Jsoup.parse(html, seriesUrl), seriesUrl)
            if (out.size > 20) break
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
                if (t.length > 200) { text = t; break }
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
                ".entry-content img, img.wp-manga-chapter-img, #chapter-content img"
        )
        val urls = imgs.mapNotNull { abs(chapterUrl, imgSrc(it)) }
            .filter { it.contains("http") && !it.contains("logo") && !it.contains("avatar") }
            .distinct()
        return urls.ifEmpty {
            doc.select("img").mapNotNull { abs(chapterUrl, imgSrc(it)) }
                .filter { it.length > 20 && it.contains("http") }.take(50)
        }
    }
}
