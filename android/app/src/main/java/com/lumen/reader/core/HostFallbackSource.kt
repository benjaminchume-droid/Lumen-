package com.lumen.reader.core

/**
 * Last-resort ContentSource that uses CatalogService (Jsoup/AJAX).
 * Registered only when Mihon reflection and LNReader JS evaluation fail.
 * UI must never call CatalogService directly.
 */
class HostFallbackSource(
    private val entry: IndexEntry
) : ContentSource {

    override val manifest = ExtensionManifest(
        id = entry.id,
        name = entry.name,
        version = entry.version,
        lang = entry.lang,
        kind = entry.kind,
        baseUrl = entry.site.orEmpty(),
        nsfw = entry.nsfw,
        iconUrl = entry.iconUrl,
        pkg = entry.pkg
    )

    override suspend fun getPopular(page: Int): SearchPage {
        val list = CatalogService.fetchPopular(entry, limit = 40)
        return SearchPage(
            items = list.map { it.toSeriesMeta() },
            hasNextPage = list.size >= 40
        )
    }

    override suspend fun getLatest(page: Int): SearchPage = getPopular(page)

    override suspend fun search(query: String, page: Int): SearchPage {
        val all = CatalogService.fetchPopular(entry, limit = 80)
        val q = query.trim().lowercase()
        val filtered = if (q.isBlank()) all else all.filter { it.title.lowercase().contains(q) }
        return SearchPage(filtered.map { it.toSeriesMeta() }, hasNextPage = false)
    }

    override suspend fun getDetails(seriesUrl: String): SeriesMeta {
        val (series, _) = CatalogService.fetchDetailsAndChapters(seriesUrl, entry)
        return series.toSeriesMeta()
    }

    override suspend fun getChapters(seriesUrl: String): List<ChapterMeta> {
        val (_, chapters) = CatalogService.fetchDetailsAndChapters(seriesUrl, entry)
        return chapters.map {
            ChapterMeta(
                url = it.url,
                name = it.title,
                number = it.number,
                sourceId = entry.id
            )
        }
    }

    override suspend fun getContent(chapterUrl: String): ContentPayload {
        val isNovel = entry.kind == MediaKind.NOVEL
        val pages = CatalogService.fetchChapterPages(chapterUrl, isNovel)
        return if (isNovel) {
            ContentPayload.Text(
                title = null,
                blocks = pages.mapIndexed { i, t -> TextBlock(i, t) }
            )
        } else {
            ContentPayload.Images(
                pages = pages.mapIndexed { i, url ->
                    ImagePage(
                        index = i,
                        imageUrl = url,
                        headers = mapOf("Referer" to (entry.site ?: chapterUrl))
                    )
                }
            )
        }
    }

    private fun CatalogService.LiveSeries.toSeriesMeta() = SeriesMeta(
        url = url,
        title = title,
        author = author.ifBlank { null },
        description = description.ifBlank { null },
        coverUrl = coverUrl,
        genres = genres,
        kind = if (kind.equals("novel", true)) MediaKind.NOVEL else MediaKind.MANGA,
        sourceId = sourceId
    )
}
