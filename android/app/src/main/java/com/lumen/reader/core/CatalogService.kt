package com.lumen.reader.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

/**
 * Live catalog host: fetches real series/chapters from the site URL
 * stored on each installed IndexEntry (Keiyoushi homeUrl / LNReader site).
 *
 * Uses Madara-style selectors (majority of Keiyoushi manga sources) plus
 * generic fallbacks. Not a full DexClassLoader of extension APKs — that
 * needs the full Mihon extensions-lib — but produces real titles and text.
 */
object CatalogService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private fun httpGet(url: String): String? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml")
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
        if (href.isNullOrBlank()) return null
        if (href.startsWith("http")) return href
        val root = base.trimEnd('/')
        return if (href.startsWith("/")) "$root$href" else "$root/$href"
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

    suspend fun fetchPopularFromInstalled(entries: List<IndexEntry>, perSource: Int = 12): List<LiveSeries> =
        withContext(Dispatchers.IO) {
            coroutineScope {
                entries.map { entry ->
                    async {
                        runCatching { fetchPopular(entry, perSource) }.getOrDefault(emptyList())
                    }
                }.awaitAll().flatten()
            }
        }

    fun fetchPopular(entry: IndexEntry, limit: Int = 12): List<LiveSeries> {
        val base = entry.site?.trim()?.trimEnd('/') ?: return emptyList()
        if (!base.startsWith("http")) return emptyList()

        val candidates = listOf(
            "$base/manga/?m_orderby=views",
            "$base/manga/page/1/?m_orderby=views",
            "$base/manga/",
            "$base/?s=&post_type=wp-manga",
            base
        )
        for (url in candidates) {
            val html = httpGet(url) ?: continue
            val parsed = parseSeriesList(html, base, entry, limit)
            if (parsed.isNotEmpty()) return parsed
        }
        return emptyList()
    }

    private fun parseSeriesList(html: String, base: String, entry: IndexEntry, limit: Int): List<LiveSeries> {
        val doc = Jsoup.parse(html, base)
        val out = LinkedHashMap<String, LiveSeries>()

        val cards = doc.select(
            ".page-item-detail, .manga-item, .bs, .bsx, .listupd .utao, " +
                ".c-tabs-item__content, .row.c-tabs-item, .manga, .item, article.post"
        )
        for (card in cards) {
            if (out.size >= limit) break
            val a = card.selectFirst("a[href*=/manga/], a[href*=/manhwa/], a[href*=/manhua/], h3 a, h5 a, .post-title a, a.tip")
                ?: card.selectFirst("a[href]")
                ?: continue
            val href = abs(base, a.attr("href")) ?: continue
            if (href.contains("/chapter") || href.contains("/ch-")) continue
            val title = a.attr("title").ifBlank { a.text() }.ifBlank {
                card.selectFirst("h3, h5, .post-title, .tt")?.text().orEmpty()
            }.trim()
            if (title.isBlank() || title.length < 2) continue
            val img = card.selectFirst("img")
            val cover = abs(base, img?.attr("data-src")?.takeIf { it.isNotBlank() }
                ?: img?.attr("data-lazy-src")?.takeIf { it.isNotBlank() }
                ?: img?.attr("src"))
            val id = "${entry.id}|$href"
            if (!out.containsKey(id)) {
                out[id] = LiveSeries(
                    id = id,
                    title = title,
                    url = href,
                    coverUrl = cover,
                    sourceName = entry.name,
                    sourceId = entry.id,
                    kind = if (entry.kind == MediaKind.NOVEL) "novel" else "manga"
                )
            }
        }

        if (out.size < 3) {
            for (a in doc.select("a[href*=/manga/], a[href*=/novel/], a[href*=/book/]")) {
                if (out.size >= limit) break
                val href = abs(base, a.attr("href")) ?: continue
                if (href.contains("/chapter") || href.endsWith("/manga/") || href.endsWith("/novel/")) continue
                val title = a.attr("title").ifBlank { a.text() }.trim()
                if (title.length < 3) continue
                val img = a.selectFirst("img") ?: a.parent()?.selectFirst("img")
                val cover = abs(base, img?.attr("data-src")?.takeIf { it.isNotBlank() } ?: img?.attr("src"))
                val id = "${entry.id}|$href"
                if (!out.containsKey(id)) {
                    out[id] = LiveSeries(
                        id = id,
                        title = title.take(120),
                        url = href,
                        coverUrl = cover,
                        sourceName = entry.name,
                        sourceId = entry.id,
                        kind = if (entry.kind == MediaKind.NOVEL || href.contains("/novel")) "novel" else "manga"
                    )
                }
            }
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
        val title = doc.selectFirst("h1, .post-title h1, .manga-title, .entry-title")?.text()?.trim()
            ?: doc.title().substringBefore("|").trim()
        val cover = abs(
            seriesUrl,
            doc.selectFirst(".summary_image img, .thumb img, .manga-thumb img, meta[property=og:image]")
                ?.let { if (it.tagName() == "meta") it.attr("content") else it.attr("data-src").ifBlank { it.attr("src") } }
        )
        val desc = doc.selectFirst(".description-summary, .summary__content, .manga-excerpt, .entry-content p, meta[name=description]")
            ?.let { if (it.tagName() == "meta") it.attr("content") else it.text() }?.trim().orEmpty()
        val author = doc.selectFirst(".author-content a, .artist-content a, .manga-author")?.text()?.trim().orEmpty()
        val genres = doc.select(".genres-content a, .mgen a, a[rel=tag]").map { it.text().trim() }.filter { it.isNotBlank() }.take(8)

        val chapters = LinkedHashMap<String, LiveChapter>()
        val selectors = listOf(
            "li.wp-manga-chapter a",
            ".listing-chapters_wrap a",
            ".chapter-list a",
            "ul.main li a",
            ".eplister a",
            "#chapterlist a",
            "a[href*=/chapter], a[href*=/ch-]"
        )
        var n = 0f
        for (sel in selectors) {
            for (a in doc.select(sel)) {
                val href = abs(seriesUrl, a.attr("href")) ?: continue
                val name = a.text().trim().ifBlank { a.attr("title") }.ifBlank { "Chapter" }
                if (name.length < 1) continue
                if (chapters.containsKey(href)) continue
                val numMatch = Regex("""(\d+(?:\.\d+)?)""").find(name)
                val num = numMatch?.groupValues?.get(1)?.toFloatOrNull() ?: (++n)
                chapters[href] = LiveChapter(id = href, title = name.take(80), url = href, number = num)
            }
            if (chapters.size > 5) break
        }
        val sorted = chapters.values.sortedByDescending { it.number }
        val series = LiveSeries(
            id = "${entry.id}|$seriesUrl",
            title = title.ifBlank { "Series" },
            url = seriesUrl,
            coverUrl = cover,
            sourceName = entry.name,
            sourceId = entry.id,
            kind = if (entry.kind == MediaKind.NOVEL) "novel" else "manga",
            author = author,
            description = desc.take(800),
            genres = genres
        )
        return series to sorted
    }

    fun fetchChapterPages(chapterUrl: String, isNovel: Boolean): List<String> {
        val html = httpGet(chapterUrl) ?: return emptyList()
        val doc = Jsoup.parse(html, chapterUrl)
        if (isNovel) {
            val body = doc.selectFirst(
                ".reading-content, .text-left, .chapter-content, #chapter-content, " +
                    ".entry-content, .content-area, article .content, #content"
            )
            val text = body?.text()?.trim().orEmpty()
            if (text.length > 80) {
                return text.split(Regex("(?<=[.!?])\\s+"))
                    .chunked(6)
                    .map { it.joinToString(" ") }
                    .filter { it.length > 20 }
            }
            return listOf(text.ifBlank { "Could not extract chapter text from this source." })
        }
        val imgs = doc.select(
            ".reading-content img, .page-break img, #readerarea img, .chapter-content img, " +
                ".entry-content img, img.wp-manga-chapter-img"
        )
        val urls = imgs.mapNotNull { img ->
            abs(
                chapterUrl,
                img.attr("data-src").ifBlank { img.attr("data-lazy-src") }.ifBlank { img.attr("src") }
            )
        }.filter { it.contains("http") && !it.contains("logo") && !it.contains("avatar") }
            .distinct()
        return urls.ifEmpty {
            doc.select("img").mapNotNull { abs(chapterUrl, it.attr("src")) }
                .filter { it.length > 20 }.take(30)
        }
    }
}
