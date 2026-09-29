package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Convenience base for HTML sites — extensions implement selectors + fromElement.
 */
abstract class ParsedHttpSource : HttpSource() {

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select(popularMangaSelector()).map { popularMangaFromElement(it) }
        val hasNext = popularMangaNextPageSelector()?.let { document.select(it).first() } != null
        return MangasPage(mangas, hasNext)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select(searchMangaSelector()).map { searchMangaFromElement(it) }
        val hasNext = searchMangaNextPageSelector()?.let { document.select(it).first() } != null
        return MangasPage(mangas, hasNext)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select(latestUpdatesSelector()).map { latestUpdatesFromElement(it) }
        val hasNext = latestUpdatesNextPageSelector()?.let { document.select(it).first() } != null
        return MangasPage(mangas, hasNext)
    }

    override fun mangaDetailsParse(response: Response): SManga =
        mangaDetailsParse(response.asJsoup())

    abstract fun mangaDetailsParse(document: Document): SManga

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        return document.select(chapterListSelector()).map { chapterFromElement(it) }
    }

    override fun pageListParse(response: Response): List<Page> =
        pageListParse(response.asJsoup())

    open fun pageListParse(document: Document): List<Page> = emptyList()

    abstract fun popularMangaSelector(): String
    abstract fun popularMangaFromElement(element: Element): SManga
    open fun popularMangaNextPageSelector(): String? = null

    abstract fun searchMangaSelector(): String
    abstract fun searchMangaFromElement(element: Element): SManga
    open fun searchMangaNextPageSelector(): String? = null

    abstract fun latestUpdatesSelector(): String
    abstract fun latestUpdatesFromElement(element: Element): SManga
    open fun latestUpdatesNextPageSelector(): String? = null

    abstract fun chapterListSelector(): String
    abstract fun chapterFromElement(element: Element): SChapter
}

fun Response.asJsoup(): Document {
    val body = body?.string().orEmpty()
    val base = request.url.toString()
    return Jsoup.parse(body, base)
}
