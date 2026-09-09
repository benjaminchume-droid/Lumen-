package com.lumen.reader.core

enum class MediaKind { MANGA, NOVEL }

data class ExtensionManifest(
    val id: String,
    val name: String,
    val version: String,
    val lang: String,
    val kind: MediaKind,
    val baseUrl: String,
    val nsfw: Boolean = false,
    val iconUrl: String? = null
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
    val kind: MediaKind
)

data class ChapterMeta(
    val url: String,
    val name: String,
    val number: Float,
    val scanlator: String? = null,
    val uploadedAt: Long? = null,
    val sourceId: String
)

data class Page(val index: Int, val url: String, val headers: Map<String, String> = emptyMap())
data class NovelPage(val index: Int, val content: String)

interface Source {
    val id: String
    val name: String
    val lang: String
    val kind: MediaKind
    val baseUrl: String

    suspend fun getPopular(page: Int): Pair<List<SeriesMeta>, Boolean>
    suspend fun getLatest(page: Int): Pair<List<SeriesMeta>, Boolean>
    suspend fun search(query: String, page: Int): Pair<List<SeriesMeta>, Boolean>
    suspend fun getSeriesDetails(seriesUrl: String): SeriesMeta
    suspend fun getChapterList(seriesUrl: String): List<ChapterMeta>
    suspend fun getPageList(chapterUrl: String): List<Any>
}

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

data class LinkedSource(
    val sourceId: String,
    val seriesUrl: String,
    val priority: Int
)
