package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import rx.Observable

interface Source {
    val id: Long
    val name: String

    suspend fun getMangaDetails(manga: SManga): SManga =
        throw UnsupportedOperationException("getMangaDetails")

    suspend fun getChapterList(manga: SManga): List<SChapter> =
        throw UnsupportedOperationException("getChapterList")

    suspend fun getPageList(chapter: SChapter): List<Page> =
        throw UnsupportedOperationException("getPageList")

    @Deprecated("Use the non-RxJava API instead", ReplaceWith("getMangaDetails"))
    fun fetchMangaDetails(manga: SManga): Observable<SManga> =
        Observable.error(UnsupportedOperationException("fetchMangaDetails"))

    @Deprecated("Use the non-RxJava API instead", ReplaceWith("getChapterList"))
    fun fetchChapterList(manga: SManga): Observable<List<SChapter>> =
        Observable.error(UnsupportedOperationException("fetchChapterList"))

    @Deprecated("Use the non-RxJava API instead", ReplaceWith("getPageList"))
    fun fetchPageList(chapter: SChapter): Observable<List<Page>> =
        Observable.error(UnsupportedOperationException("fetchPageList"))
}

interface CatalogueSource : Source {
    val lang: String
    val supportsLatest: Boolean

    suspend fun getPopularManga(page: Int): MangasPage =
        throw UnsupportedOperationException("getPopularManga")

    suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage =
        throw UnsupportedOperationException("getSearchManga")

    suspend fun getLatestUpdates(page: Int): MangasPage =
        throw UnsupportedOperationException("getLatestUpdates")

    fun getFilterList(): FilterList = FilterList()

    @Deprecated("Use the non-RxJava API instead", ReplaceWith("getPopularManga"))
    fun fetchPopularManga(page: Int): Observable<MangasPage> =
        Observable.error(UnsupportedOperationException("fetchPopularManga"))

    @Deprecated("Use the non-RxJava API instead", ReplaceWith("getSearchManga"))
    fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> =
        Observable.error(UnsupportedOperationException("fetchSearchManga"))

    @Deprecated("Use the non-RxJava API instead", ReplaceWith("getLatestUpdates"))
    fun fetchLatestUpdates(page: Int): Observable<MangasPage> =
        Observable.error(UnsupportedOperationException("fetchLatestUpdates"))
}
