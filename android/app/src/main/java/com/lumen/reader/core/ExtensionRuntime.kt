package com.lumen.reader.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dalvik.system.PathClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Mihon + LNReader runtime host.
 * Keiyoushi: PathClassLoader + reflective Source APIs; CatalogService site fallback.
 * LNReader: JS on disk; CatalogService against plugin site.
 */
class ExtensionRuntime(private val context: Context) {

    private val host = ExtensionHost(context)
    private val pluginDir = File(context.filesDir, "plugins").also { it.mkdirs() }

    fun pluginFile(id: String): File =
        File(pluginDir, "${id.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.js")

    fun isLnPluginOnDisk(id: String): Boolean =
        pluginFile(id).exists() && pluginFile(id).length() > 20

    fun loadedExtensions(): List<ExtensionHost.LoadedExt> = host.findInstalledExtensions()

    suspend fun fetchAllPopular(
        installed: List<IndexEntry>,
        perSource: Int = 80,
        maxConcurrent: Int = 6
    ): List<CatalogService.LiveSeries> = withContext(Dispatchers.IO) {
        if (installed.isEmpty()) return@withContext emptyList()
        val sem = Semaphore(maxConcurrent)
        coroutineScope {
            installed.map { entry ->
                async {
                    sem.withPermit {
                        runCatching { fetchOne(entry, perSource) }.getOrDefault(emptyList())
                    }
                }
            }.awaitAll().flatten()
        }
    }

    private fun fetchOne(entry: IndexEntry, limit: Int): List<CatalogService.LiveSeries> {
        if (entry.kind == MediaKind.MANGA && !entry.pkg.isNullOrBlank()) {
            val reflected = tryReflectPopular(entry, limit)
            if (reflected.isNotEmpty()) return reflected
        }
        if (!entry.site.isNullOrBlank() && entry.site!!.startsWith("http")) {
            return CatalogService.fetchPopular(entry, limit)
        }
        return emptyList()
    }

    fun tryReflectChapters(entry: IndexEntry, seriesUrl: String): List<CatalogService.LiveChapter> {
        val pkg = entry.pkg ?: return emptyList()
        return try {
            val loader = classLoaderFor(pkg) ?: return emptyList()
            val sources = instantiateSources(pkg, loader)
            val out = ArrayList<CatalogService.LiveChapter>()
            for (src in sources) {
                val list = invokeChapterList(src, seriesUrl) ?: continue
                out.addAll(list)
            }
            out.distinctBy { it.url }.sortedBy { it.number }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun classLoaderFor(pkg: String): PathClassLoader? {
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

    private fun instantiateSources(pkg: String, loader: PathClassLoader): List<Any> {
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
                val clazz = Class.forName(cn, false, loader)
                val instance = clazz.getDeclaredConstructor().newInstance()
                val created = when {
                    instance.javaClass.methods.any { it.name == "createSources" } -> {
                        @Suppress("UNCHECKED_CAST")
                        (clazz.getMethod("createSources").invoke(instance) as? List<*>).orEmpty()
                            .filterNotNull()
                    }
                    else -> listOf(instance)
                }
                out.addAll(created)
            } catch (_: Throwable) {
            }
        }
        return out
    }

    private fun tryReflectPopular(entry: IndexEntry, limit: Int): List<CatalogService.LiveSeries> {
        val pkg = entry.pkg ?: return emptyList()
        return try {
            val loader = classLoaderFor(pkg) ?: return emptyList()
            val sources = instantiateSources(pkg, loader)
            val out = ArrayList<CatalogService.LiveSeries>()
            for (src in sources) {
                if (out.size >= limit) break
                val page = tryInvokePopular(src, 1) ?: continue
                for (m in page) {
                    if (out.size >= limit) break
                    out.add(m.copy(sourceName = entry.name, sourceId = entry.id, kind = "manga"))
                }
            }
            out
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun tryInvokePopular(source: Any, page: Int): List<CatalogService.LiveSeries>? {
        return try {
            val methods = source.javaClass.methods
            val fetch = methods.firstOrNull {
                it.name == "fetchPopularManga" && it.parameterTypes.size == 1
            }
            val get = methods.firstOrNull {
                it.name == "getPopularManga" && it.parameterTypes.size == 1
            }
            val raw: Any = when {
                fetch != null -> fetch.invoke(source, page) ?: return null
                get != null -> get.invoke(source, page) ?: return null
                else -> return null
            }

            val mangas: List<*> = when (raw) {
                is List<*> -> raw
                else -> {
                    val field = raw.javaClass.fields.firstOrNull { it.name == "mangas" }
                    val method = raw.javaClass.methods.firstOrNull {
                        it.name == "getMangas" || it.name == "mangas"
                    }
                    when {
                        field != null -> field.get(raw) as? List<*>
                        method != null -> method.invoke(raw) as? List<*>
                        else -> null
                    } ?: return null
                }
            }

            mangas.mapNotNull { m ->
                if (m == null) return@mapNotNull null
                val title = readProp(m, "title", "name") ?: return@mapNotNull null
                val url = readProp(m, "url", "path") ?: ""
                val thumb = readProp(m, "thumbnail_url", "thumbnailUrl", "cover")
                CatalogService.LiveSeries(
                    id = "${source.javaClass.name}|$url|$title",
                    title = title,
                    url = url,
                    coverUrl = thumb,
                    sourceName = readProp(source, "name") ?: source.javaClass.simpleName,
                    sourceId = source.javaClass.name,
                    kind = "manga"
                )
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun invokeChapterList(source: Any, seriesUrl: String): List<CatalogService.LiveChapter>? {
        return try {
            val methods = source.javaClass.methods
            val get = methods.firstOrNull {
                (it.name == "getChapterList" || it.name == "fetchChapterList") &&
                    it.parameterTypes.size == 1
            } ?: return null
            val argType = get.parameterTypes[0]
            val arg: Any = when {
                argType == String::class.java -> seriesUrl
                else -> {
                    try {
                        val inst = argType.getDeclaredConstructor().newInstance()
                        argType.fields.firstOrNull { it.name.equals("url", true) }?.set(inst, seriesUrl)
                        argType.methods.firstOrNull {
                            it.name.equals("setUrl", true) && it.parameterTypes.size == 1
                        }?.invoke(inst, seriesUrl)
                        inst
                    } catch (_: Throwable) {
                        seriesUrl
                    }
                }
            }
            val raw = get.invoke(source, arg) ?: return null
            val list: List<*> = when (raw) {
                is List<*> -> raw
                else -> {
                    val m = raw.javaClass.methods.firstOrNull {
                        it.name == "toList" || it.name == "blockingFirst"
                    }
                    (m?.invoke(raw) as? List<*>) ?: return null
                }
            }
            list.mapIndexedNotNull { i, ch ->
                if (ch == null) return@mapIndexedNotNull null
                val name = readProp(ch, "name", "title", "chapter_name") ?: "Chapter ${i + 1}"
                val url = readProp(ch, "url", "path") ?: return@mapIndexedNotNull null
                val num = readProp(ch, "chapter_number", "number", "chapterNumber")
                    ?.toFloatOrNull() ?: (i + 1).toFloat()
                CatalogService.LiveChapter(id = url, title = name, url = url, number = num)
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun readProp(obj: Any, vararg names: String): String? {
        for (n in names) {
            try {
                val f = obj.javaClass.fields.firstOrNull { it.name.equals(n, true) }
                if (f != null) {
                    val v = f.get(obj)?.toString()
                    if (!v.isNullOrBlank()) return v
                }
            } catch (_: Throwable) {
            }
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
            } catch (_: Throwable) {
            }
        }
        return null
    }
}
