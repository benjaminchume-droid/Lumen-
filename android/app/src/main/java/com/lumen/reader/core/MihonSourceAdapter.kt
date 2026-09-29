package com.lumen.reader.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dalvik.system.PathClassLoader
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt

/**
 * Adapts an installed Keiyoushi/Mihon extension APK into Lumen [ContentSource].
 * Primary path: reflect real extension methods (suspend or Rx Observable).
 * On total failure callers should fall back to [HostFallbackSource].
 */
class MihonSourceAdapter(
    private val context: Context,
    private val entry: IndexEntry,
    private val sourceInstance: Any,
    private val classLoader: PathClassLoader
) : ContentSource {

    override val manifest = ExtensionManifest(
        id = entry.id,
        name = entry.name,
        version = entry.version,
        lang = entry.lang,
        kind = MediaKind.MANGA,
        baseUrl = entry.site.orEmpty(),
        nsfw = entry.nsfw,
        iconUrl = entry.iconUrl,
        pkg = entry.pkg
    )

    init {
        ensureHostInjekt()
    }

    override suspend fun getPopular(page: Int): SearchPage = withContext(Dispatchers.IO) {
        val raw = invokeAny(
            listOf("getPopularManga", "fetchPopularManga"),
            arrayOf(page)
        ) ?: return@withContext SearchPage(emptyList(), false)
        parseMangasPage(raw)
    }

    override suspend fun getLatest(page: Int): SearchPage = withContext(Dispatchers.IO) {
        val raw = invokeAny(
            listOf("getLatestUpdates", "fetchLatestUpdates"),
            arrayOf(page)
        ) ?: return@withContext getPopular(page)
        parseMangasPage(raw)
    }

    override suspend fun search(query: String, page: Int): SearchPage = withContext(Dispatchers.IO) {
        val filters = try {
            val m = sourceInstance.javaClass.methods.firstOrNull {
                it.name == "getFilterList" && it.parameterTypes.isEmpty()
            }
            m?.invoke(sourceInstance)
        } catch (_: Throwable) { null }
        val raw = when {
            filters != null -> invokeAny(
                listOf("getSearchManga", "fetchSearchManga"),
                arrayOf(page, query, filters)
            )
            else -> invokeAny(
                listOf("getSearchManga", "fetchSearchManga"),
                arrayOf(page, query)
            )
        } ?: return@withContext SearchPage(emptyList(), false)
        parseMangasPage(raw)
    }

    override suspend fun getDetails(seriesUrl: String): SeriesMeta = withContext(Dispatchers.IO) {
        val manga = SManga.create().apply { url = seriesUrl; title = "" }
        val raw = invokeAny(
            listOf("getMangaDetails", "fetchMangaDetails"),
            arrayOf(manga)
        ) ?: return@withContext SeriesMeta(
            url = seriesUrl, title = "Unknown", kind = MediaKind.MANGA, sourceId = entry.id
        )
        toSeriesMeta(raw, seriesUrl)
    }

    override suspend fun getChapters(seriesUrl: String): List<ChapterMeta> = withContext(Dispatchers.IO) {
        val manga = SManga.create().apply { url = seriesUrl }
        val raw = invokeAny(
            listOf("getChapterList", "fetchChapterList"),
            arrayOf(manga)
        ) ?: return@withContext emptyList()
        parseChapterList(raw)
    }

    override suspend fun getContent(chapterUrl: String): ContentPayload = withContext(Dispatchers.IO) {
        val chapter = SChapter.create().apply { url = chapterUrl; name = "" }
        val raw = invokeAny(
            listOf("getPageList", "fetchPageList"),
            arrayOf(chapter)
        ) ?: return@withContext ContentPayload.Images(emptyList())
        parsePageList(raw, chapterUrl)
    }

    private fun invokeAny(names: List<String>, args: Array<Any?>): Any? {
        val methods = sourceInstance.javaClass.methods
        for (name in names) {
            val candidates = methods.filter { it.name == name }
            for (m in candidates) {
                try {
                    val params = m.parameterTypes
                    if (params.size != args.size && !(params.size == args.size + 1)) continue
                    val callArgs = Array(params.size) { i ->
                        if (i < args.size) coerce(args[i], params[i]) else null
                    }
                    val result = m.invoke(sourceInstance, *callArgs) ?: continue
                    return unwrapRxOrSuspend(result)
                } catch (_: Throwable) {
                    continue
                }
            }
        }
        return null
    }

    private fun coerce(value: Any?, target: Class<*>): Any? {
        if (value == null) return null
        if (target.isInstance(value)) return value
        if (target == Int::class.javaPrimitiveType || target == Int::class.java) {
            return when (value) {
                is Number -> value.toInt()
                else -> value
            }
        }
        return value
    }

    private fun unwrapRxOrSuspend(result: Any): Any {
        // rx.Observable
        if (result.javaClass.name == "rx.Observable" || result.javaClass.simpleName == "Observable") {
            try {
                val blocking = result.javaClass.methods.firstOrNull {
                    it.name == "toBlocking" && it.parameterTypes.isEmpty()
                }?.invoke(result)
                if (blocking != null) {
                    val first = blocking.javaClass.methods.firstOrNull {
                        (it.name == "first" || it.name == "single") && it.parameterTypes.isEmpty()
                    }
                    if (first != null) return first.invoke(blocking) ?: result
                }
                val bf = result.javaClass.methods.firstOrNull {
                    it.name == "blockingFirst" && it.parameterTypes.isEmpty()
                }
                if (bf != null) return bf.invoke(result) ?: result
            } catch (_: Throwable) {}
        }
        return result
    }

    private fun parseMangasPage(raw: Any): SearchPage {
        val mangas: List<*> = when (raw) {
            is List<*> -> raw
            else -> {
                val f = raw.javaClass.fields.firstOrNull { it.name == "mangas" }
                val m = raw.javaClass.methods.firstOrNull {
                    (it.name == "getMangas" || it.name == "mangas") && it.parameterTypes.isEmpty()
                }
                when {
                    f != null -> f.get(raw) as? List<*>
                    m != null -> m.invoke(raw) as? List<*>
                    else -> null
                } ?: emptyList<Any>()
            }
        }
        val hasNext = try {
            val f = raw.javaClass.fields.firstOrNull { it.name == "hasNextPage" }
            val m = raw.javaClass.methods.firstOrNull {
                (it.name == "getHasNextPage" || it.name == "hasNextPage") && it.parameterTypes.isEmpty()
            }
            when {
                f != null -> f.getBoolean(raw)
                m != null -> m.invoke(raw) as? Boolean ?: false
                else -> mangas.size >= 20
            }
        } catch (_: Throwable) { mangas.size >= 20 }

        val items = mangas.mapNotNull { toSeriesMetaOrNull(it) }
        return SearchPage(items, hasNext)
    }

    private fun toSeriesMetaOrNull(m: Any?): SeriesMeta? {
        if (m == null) return null
        val title = readProp(m, "title", "name") ?: return null
        val url = readProp(m, "url", "path") ?: return null
        val thumb = readProp(m, "thumbnail_url", "thumbnailUrl", "cover")
        val author = readProp(m, "author")
        val desc = readProp(m, "description")
        val genre = readProp(m, "genre")
        return SeriesMeta(
            url = url,
            title = title,
            author = author,
            description = desc,
            coverUrl = thumb,
            genres = genre?.split(",", ";")?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty(),
            kind = MediaKind.MANGA,
            sourceId = entry.id
        )
    }

    private fun toSeriesMeta(raw: Any, fallbackUrl: String): SeriesMeta {
        return toSeriesMetaOrNull(raw) ?: SeriesMeta(
            url = fallbackUrl, title = "Unknown", kind = MediaKind.MANGA, sourceId = entry.id
        )
    }

    private fun parseChapterList(raw: Any): List<ChapterMeta> {
        val list: List<*> = when (raw) {
            is List<*> -> raw
            else -> {
                val m = raw.javaClass.methods.firstOrNull {
                    it.name == "toList" || it.name == "blockingFirst"
                }
                (m?.invoke(raw) as? List<*>) ?: emptyList<Any>()
            }
        }
        return list.mapIndexedNotNull { i, ch ->
            if (ch == null) return@mapIndexedNotNull null
            val name = readProp(ch, "name", "title", "chapter_name") ?: "Chapter ${i + 1}"
            val url = readProp(ch, "url", "path") ?: return@mapIndexedNotNull null
            val num = readProp(ch, "chapter_number", "number", "chapterNumber")?.toFloatOrNull()
                ?: (i + 1).toFloat()
            val scan = readProp(ch, "scanlator")
            val date = readProp(ch, "date_upload", "dateUpload")?.toLongOrNull()
            ChapterMeta(
                url = url,
                name = name,
                number = num,
                scanlator = scan,
                uploadedAt = date,
                sourceId = entry.id
            )
        }.sortedBy { it.number }
    }

    private fun parsePageList(raw: Any, chapterUrl: String): ContentPayload {
        val list: List<*> = when (raw) {
            is List<*> -> raw
            else -> emptyList<Any>()
        }
        val pages = list.mapIndexedNotNull { i, p ->
            if (p == null) return@mapIndexedNotNull null
            val imageUrl = readProp(p, "imageUrl", "image_url", "url") ?: return@mapIndexedNotNull null
            val intermediate = readProp(p, "url")
            val headers = mutableMapOf<String, String>()
            headers["Referer"] = entry.site ?: chapterUrl
            try {
                val hf = p.javaClass.fields.firstOrNull { it.name.equals("headers", true) }
                val hv = hf?.get(p)
                if (hv != null) {
                    val names = hv.javaClass.methods.firstOrNull { it.name == "names" }
                        ?.invoke(hv) as? List<*>
                    names?.forEach { n ->
                        val name = n?.toString() ?: return@forEach
                        val v = hv.javaClass.methods.firstOrNull {
                            it.name == "get" && it.parameterTypes.size == 1
                        }?.invoke(hv, name)?.toString()
                        if (!v.isNullOrBlank()) headers[name] = v
                    }
                }
            } catch (_: Throwable) {}
            ImagePage(
                index = i,
                imageUrl = imageUrl,
                headers = headers,
                intermediateUrl = intermediate
            )
        }
        return ContentPayload.Images(pages)
    }

    private fun readProp(obj: Any, vararg names: String): String? {
        for (n in names) {
            try {
                val f = obj.javaClass.fields.firstOrNull { it.name.equals(n, true) }
                if (f != null) {
                    val v = f.get(obj)?.toString()
                    if (!v.isNullOrBlank()) return v
                }
            } catch (_: Throwable) {}
            try {
                val getter = "get" + n.replaceFirstChar { c -> c.uppercase() }
                val m = obj.javaClass.methods.firstOrNull {
                    (it.name.equals(n, true) || it.name.equals(getter, true)) &&
                        it.parameterTypes.isEmpty()
                }
                if (m != null) {
                    val v = m.invoke(obj)?.toString()
                    if (!v.isNullOrBlank()) return v
                }
            } catch (_: Throwable) {}
        }
        return null
    }

    companion object {
        @Volatile private var injektReady = false

        fun ensureHostInjekt() {
            if (injektReady) return
            synchronized(this) {
                if (injektReady) return
                try {
                    val nh = NetworkHelper()
                    Injekt.addSingleton(nh)
                    Injekt.addSingleton(nh.client)
                } catch (_: Throwable) {}
                injektReady = true
            }
        }

        fun tryCreate(context: Context, entry: IndexEntry): MihonSourceAdapter? {
            val pkg = entry.pkg ?: return null
            return try {
                ensureHostInjekt()
                val loader = classLoaderFor(context, pkg) ?: return null
                val sources = instantiateSources(context, pkg, loader)
                val first = sources.firstOrNull() ?: return null
                MihonSourceAdapter(context, entry, first, loader)
            } catch (_: Throwable) {
                null
            }
        }

        private fun classLoaderFor(context: Context, pkg: String): PathClassLoader? {
            return try {
                val pm = context.packageManager
                val pi = if (Build.VERSION.SDK_INT >= 33) {
                    pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, PackageManager.GET_META_DATA)
                }
                val app = pi.applicationInfo ?: return null
                PathClassLoader(app.sourceDir, context.classLoader)
            } catch (_: Throwable) {
                null
            }
        }

        private fun instantiateSources(
            context: Context,
            pkg: String,
            loader: PathClassLoader
        ): List<Any> {
            val pm = context.packageManager
            val pi = if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, PackageManager.GET_META_DATA)
            }
            val meta = pi.applicationInfo?.metaData ?: return emptyList()
            val clsMeta = meta.getString("tachiyomi.extension.class")
                ?: meta.getString("tachiyomi.extension.factory")
                ?: return emptyList()
            val classNames = clsMeta.split(';').map { it.trim() }.filter { it.isNotBlank() }.map {
                if (it.startsWith(".")) pkg + it else it
            }
            val out = ArrayList<Any>()
            for (cn in classNames) {
                try {
                    val clazz = Class.forName(cn, true, loader)
                    val instance = clazz.getDeclaredConstructor().newInstance()
                    val created = when {
                        instance.javaClass.methods.any { it.name == "createSources" } -> {
                            @Suppress("UNCHECKED_CAST")
                            (clazz.getMethod("createSources").invoke(instance) as? List<*>)
                                .orEmpty().filterNotNull()
                        }
                        else -> listOf(instance)
                    }
                    out.addAll(created)
                } catch (_: Throwable) {}
            }
            return out
        }
    }
}
