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
 * Mihon + LNReader style runtime host.
 *
 * Keiyoushi (manga): PathClassLoader on installed APKs; reflective Source.getPopularManga when possible;
 * always falls back to CatalogService using homeUrl from the index (covers Madara/Themesia multisrc sites).
 *
 * LNReader (novels): plugins are JS files downloaded to filesDir/plugins; catalog uses plugin site URL
 * via CatalogService (same HTML sources the JS plugins target).
 *
 * Goal: every entry from Keiyoushi (~1377) and LNReader (~284) indexes can be installed and contribute
 * to Home when enabled — not a hand-picked subset.
 */
class ExtensionRuntime(private val context: Context) {

    private val host = ExtensionHost(context)
    private val pluginDir = File(context.filesDir, "plugins").also { it.mkdirs() }

    fun pluginFile(id: String): File = File(pluginDir, "${id.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.js")

    fun isLnPluginOnDisk(id: String): Boolean = pluginFile(id).exists() && pluginFile(id).length() > 20

    suspend fun fetchAllPopular(
        installed: List<IndexEntry>,
        perSource: Int = 60,
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

    private fun tryReflectPopular(entry: IndexEntry, limit: Int): List<CatalogService.LiveSeries> {
        val pkg = entry.pkg ?: return emptyList()
        return try {
            val pm = context.packageManager
            val pi = if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, PackageManager.GET_META_DATA)
            }
            val app = pi.applicationInfo ?: return emptyList()
            val meta = app.metaData ?: return emptyList()
            val clsMeta = meta.getString("tachiyomi.extension.class")
                ?: meta.getString("tachiyomi.extension.factory")
                ?: return emptyList()
            val loader = PathClassLoader(app.sourceDir, context.classLoader)
            val classNames = clsMeta.split(';').map { it.trim() }.filter { it.isNotBlank() }.map {
                if (it.startsWith(".")) pkg + it else it
            }
            val out = ArrayList<CatalogService.LiveSeries>()
            for (cn in classNames) {
                if (out.size >= limit) break
                val clazz = Class.forName(cn, false, loader)
                val instance = clazz.getDeclaredConstructor().newInstance()
                val sources = when {
                    instance.javaClass.methods.any { it.name == "createSources" } -> {
                        @Suppress("UNCHECKED_CAST")
                        (clazz.getMethod("createSources").invoke(instance) as? List<*>).orEmpty()
                    }
                    else -> listOf(instance)
                }
                for (src in sources) {
                    if (src == null || out.size >= limit) continue
                    val page = tryInvokePopular(src, 1) ?: continue
                    for (m in page) {
                        if (out.size >= limit) break
                        out.add(m.copy(sourceName = entry.name, sourceId = entry.id, kind = "manga"))
                    }
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
            val result: Any? = when {
                fetch != null -> fetch.invoke(source, page)
                get != null -> get.invoke(source, page)
                else -> null
            } ?: return null

            val mangas = when (result) {
                is List<*> -> result
                else -> {
                    val field = result.javaClass.fields.firstOrNull { it.name == "mangas" }
                        ?: result.javaClass.methods.firstOrNull { it.name == "getMangas" || it.name == "mangas" }
                    when (field) {
                        is java.lang.reflect.Field -> field.get(result) as? List<*>
                        is java.lang.reflect.Method -> field.invoke(result) as? List<*>
                        else -> null
                    }
                }
            } ?: return null

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
                val m = obj.javaClass.methods.firstOrNull {
                    it.name.equals(n, true) || it.name.equals("get${n.replaceFirstChar { c -> c.uppercase() }}", true)
                }
                if (m != null && m.parameterTypes.isEmpty()) {
                    val v = m.invoke(obj)?.toString()
                    if (!v.isNullOrBlank()) return v
                }
            } catch (_: Throwable) {}
        }
        return null
    }
}
