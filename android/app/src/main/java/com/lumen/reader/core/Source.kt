package com.lumen.reader.core

/**
 * Legacy thin Source interface kept for compile compatibility.
 * New code should use [ContentSource] from SourceContract.kt.
 * Types (MediaKind, SeriesMeta, ChapterMeta, Aggregator models) live in SourceContract.
 */
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
