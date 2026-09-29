package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.Injekt
import java.security.MessageDigest

abstract class HttpSource : CatalogueSource {

    override val id by lazy {
        val key = "${name.lowercase()}/$lang/$versionId"
        val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }.reduce(Long::or) and Long.MAX_VALUE
    }

    open val versionId = 1

    open val baseUrl: String = ""

    open val client: OkHttpClient
        get() = try {
            val nh = Injekt.get(NetworkHelper::class.java)
            nh.cloudflareClient
        } catch (_: Throwable) {
            try {
                Injekt.get(NetworkHelper::class.java).client
            } catch (_: Throwable) {
                NetworkHelper().cloudflareClient
            }
        }

    override val supportsLatest: Boolean = true

    val headers: Headers by lazy { headersBuilder().build() }

    open fun headersBuilder() = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")

    override fun fetchPopularManga(page: Int): Observable<MangasPage> =
        Observable.fromCallable {
            client.newCall(popularMangaRequest(page)).execute().use { popularMangaParse(it) }
        }

    open fun popularMangaRequest(page: Int): Request = GET("$baseUrl/", headers)

    open fun popularMangaParse(response: Response): MangasPage = MangasPage(emptyList(), false)

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> =
        Observable.fromCallable {
            client.newCall(searchMangaRequest(page, query, filters)).execute().use { searchMangaParse(it) }
        }

    open fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        GET("$baseUrl/?s=${java.net.URLEncoder.encode(query, "UTF-8")}", headers)

    open fun searchMangaParse(response: Response): MangasPage = MangasPage(emptyList(), false)

    override fun fetchLatestUpdates(page: Int): Observable<MangasPage> =
        Observable.fromCallable {
            client.newCall(latestUpdatesRequest(page)).execute().use { latestUpdatesParse(it) }
        }

    open fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/", headers)

    open fun latestUpdatesParse(response: Response): MangasPage = MangasPage(emptyList(), false)

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> =
        Observable.fromCallable {
            client.newCall(mangaDetailsRequest(manga)).execute().use { response ->
                mangaDetailsParse(response).apply {
                    initialized = true
                    url = manga.url
                }
            }
        }

    open fun mangaDetailsRequest(manga: SManga): Request = GET(baseUrl + manga.url, headers)

    open fun mangaDetailsParse(response: Response): SManga = SManga.create()

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> =
        Observable.fromCallable {
            client.newCall(chapterListRequest(manga)).execute().use { chapterListParse(it) }
        }

    open fun chapterListRequest(manga: SManga): Request = GET(baseUrl + manga.url, headers)

    open fun chapterListParse(response: Response): List<SChapter> = emptyList()

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> =
        Observable.fromCallable {
            client.newCall(pageListRequest(chapter)).execute().use { pageListParse(it) }
        }

    open fun pageListRequest(chapter: SChapter): Request = GET(baseUrl + chapter.url, headers)

    open fun pageListParse(response: Response): List<Page> = emptyList()

    open fun imageUrlParse(response: Response): String = ""

    open fun getMangaUrl(manga: SManga): String = baseUrl + manga.url
    open fun getChapterUrl(chapter: SChapter): String = baseUrl + chapter.url

    override fun getFilterList(): FilterList = FilterList()
}
