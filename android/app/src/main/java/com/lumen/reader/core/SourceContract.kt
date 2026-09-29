package com.lumen.reader.core

/**
 * Lumen universal content contract.
 * UI and downloads call only these methods — never site-specific scrapers.
 */

enum class MediaKind { MANGA, NOVEL }

data class ExtensionManifest(
    val id: String,
    val name: String,
    val version: String,
    val lang: String,
    val kind: MediaKind,
    val baseUrl: String,
    val nsfw: Boolean = false,
    val iconUrl: String? = null,
    val pkg: String? = null
)

data class SeriesMeta(
    val url: String,
    val title: String,
    val altTitles: List<String> = emptyList(),
    val author: String? = null,
    val artist: String? = null,
    val description: String? = null,
    val coverUrl: String? = null,
    val genres: List<String> = emptyList(),
    val status: String = "unknown",
    val kind: MediaKind,
    val sourceId: String = ""
)

data class ChapterMeta(
    val url: String,
    val name: String,
    val number: Float,
    val scanlator: String? = null,
    val uploadedAt: Long? = null,
    val sourceId: String
)

data class ImagePage(
    val index: Int,
    val imageUrl: String,
    val headers: Map<String, String> = emptyMap(),
    val intermediateUrl: String? = null
)

data class TextBlock(
    val index: Int,
    val text: String
)

sealed class ContentPayload {
    data class Images(val pages: List<ImagePage>) : ContentPayload()
    data class Text(val title: String?, val blocks: List<TextBlock>) : ContentPayload()
    data class Epub(val fileUrl: String) : ContentPayload()
}

data class SearchPage(
    val items: List<SeriesMeta>,
    val hasNextPage: Boolean
)

/**
 * Plugin-facing source. Every installed Mihon extension and LNReader plugin
 * is adapted into this interface. HostFallbackSource is last resort only.
 */
interface ContentSource {
    val manifest: ExtensionManifest

    suspend fun getPopular(page: Int): SearchPage
    suspend fun getLatest(page: Int): SearchPage
    suspend fun search(query: String, page: Int): SearchPage
    suspend fun getDetails(seriesUrl: String): SeriesMeta
    suspend fun getChapters(seriesUrl: String): List<ChapterMeta>
    suspend fun getContent(chapterUrl: String): ContentPayload
}

data class SourceHealth(
    val sourceId: String,
    val lastSuccessAt: Long? = null,
    val lastFailureAt: Long? = null,
    val lastError: String? = null,
    val lastHttpStatus: Int? = null,
    val successCount: Int = 0,
    val failureCount: Int = 0
)

data class SourcePriority(
    val sourceId: String,
    val priority: Int,
    val qualityScore: Int
)

data class AggregatedChapter(
    val number: Float,
    val name: String,
    val primary: ChapterMeta,
    val mirrors: List<ChapterMeta>,
    val qualityScore: Int
)

data class LinkedSource(
    val sourceId: String,
    val seriesUrl: String,
    val priority: Int
)

data class AggregatedSeries(
    val id: String,
    val title: String,
    val kind: MediaKind,
    val coverUrl: String? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val status: String = "unknown",
    val linkedSources: List<LinkedSource> = emptyList(),
    val chapters: List<AggregatedChapter> = emptyList(),
    val lastSyncedAt: Long = System.currentTimeMillis()
)
