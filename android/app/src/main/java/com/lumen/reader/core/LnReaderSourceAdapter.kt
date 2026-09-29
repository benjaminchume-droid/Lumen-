package com.lumen.reader.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.javascript.Context as RhinoContext
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * LNReader-style JS plugin adapter.
 * Plugins live in filesDir/plugins/{id}.js
 * Host bindings: fetch, storage, absoluteUrl
 * If JS evaluation fails or plugin missing, returns null so registry uses HostFallbackSource.
 */
class LnReaderSourceAdapter(
    private val context: Context,
    private val entry: IndexEntry,
    private val pluginJs: String
) : ContentSource {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .build()

    private val storageFile = File(context.filesDir, "plugin_storage/${entry.id}.json").also {
        it.parentFile?.mkdirs()
    }

    override val manifest = ExtensionManifest(
        id = entry.id,
        name = entry.name,
        version = entry.version,
        lang = entry.lang,
        kind = MediaKind.NOVEL,
        baseUrl = entry.site.orEmpty(),
        nsfw = entry.nsfw,
        iconUrl = entry.iconUrl
    )

    override suspend fun getPopular(page: Int): SearchPage = withContext(Dispatchers.IO) {
        val result = evalFunction("search", "\"\"", page)
            ?: evalFunction("popularNovels", page.toString())
            ?: return@withContext SearchPage(emptyList(), false)
        parseSearch(result)
    }

    override suspend fun getLatest(page: Int): SearchPage = getPopular(page)

    override suspend fun search(query: String, page: Int): SearchPage = withContext(Dispatchers.IO) {
        val q = JSONObject.quote(query)
        val result = evalFunction("search", q, page)
            ?: return@withContext SearchPage(emptyList(), false)
        parseSearch(result)
    }

    override suspend fun getDetails(seriesUrl: String): SeriesMeta = withContext(Dispatchers.IO) {
        val arg = JSONObject.quote(seriesUrl)
        val result = evalFunction("getNovelDetails", arg)
            ?: evalFunction("parseNovelAndChapters", arg)
            ?: return@withContext SeriesMeta(
                url = seriesUrl, title = "Unknown", kind = MediaKind.NOVEL, sourceId = entry.id
            )
        parseDetails(result, seriesUrl)
    }

    override suspend fun getChapters(seriesUrl: String): List<ChapterMeta> = withContext(Dispatchers.IO) {
        val arg = JSONObject.quote(seriesUrl)
        val result = evalFunction("getChapters", arg)
            ?: evalFunction("parseNovelAndChapters", arg)
            ?: return@withContext emptyList()
        parseChapters(result)
    }

    override suspend fun getContent(chapterUrl: String): ContentPayload = withContext(Dispatchers.IO) {
        val arg = JSONObject.quote(chapterUrl)
        val result = evalFunction("getChapterContent", arg)
            ?: evalFunction("parseChapter", arg)
            ?: return@withContext ContentPayload.Text(null, emptyList())
        parseContent(result)
    }

    private fun evalFunction(name: String, vararg args: Any): String? {
        return try {
            val cx = RhinoContext.enter()
            cx.optimizationLevel = -1
            try {
                val scope = cx.initStandardObjects()
                installHostBindings(cx, scope)
                cx.evaluateString(scope, pluginJs, entry.id, 1, null)
                val fn = scope.get(name, scope)
                if (fn == Scriptable.NOT_FOUND || fn == null) return null
                val argObjs = args.map { RhinoContext.javaToJS(it, scope) }.toTypedArray()
                val out = (fn as org.mozilla.javascript.Function).call(cx, scope, scope, argObjs)
                when (out) {
                    null, Scriptable.NOT_FOUND -> null
                    is String -> out
                    else -> {
                        // stringify JS objects
                        val json = scope.get("JSON", scope) as? Scriptable
                        val stringify = json?.get("stringify", json) as? org.mozilla.javascript.Function
                        stringify?.call(cx, scope, json, arrayOf(out))?.toString()
                            ?: out.toString()
                    }
                }
            } finally {
                RhinoContext.exit()
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun installHostBindings(cx: RhinoContext, scope: Scriptable) {
        // fetch(url) → string body
        ScriptableObject.putProperty(scope, "fetch", object : org.mozilla.javascript.BaseFunction() {
            override fun call(
                cx: RhinoContext,
                scope: Scriptable,
                thisObj: Scriptable,
                args: Array<out Any>
            ): Any {
                val url = args.firstOrNull()?.toString() ?: return ""
                return try {
                    val req = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) Chrome/120.0.0.0 Mobile Safari/537.36")
                        .build()
                    client.newCall(req).execute().use { it.body?.string().orEmpty() }
                } catch (_: Throwable) {
                    ""
                }
            }
        })
        // absoluteUrl(base, path)
        ScriptableObject.putProperty(scope, "absoluteUrl", object : org.mozilla.javascript.BaseFunction() {
            override fun call(
                cx: RhinoContext,
                scope: Scriptable,
                thisObj: Scriptable,
                args: Array<out Any>
            ): Any {
                val base = args.getOrNull(0)?.toString().orEmpty()
                val path = args.getOrNull(1)?.toString().orEmpty()
                if (path.startsWith("http")) return path
                if (path.startsWith("//")) return "https:$path"
                val root = base.trimEnd('/')
                return if (path.startsWith("/")) "$root$path" else "$root/$path"
            }
        })
        // plugin storage get/set
        ScriptableObject.putProperty(scope, "storageGet", object : org.mozilla.javascript.BaseFunction() {
            override fun call(
                cx: RhinoContext,
                scope: Scriptable,
                thisObj: Scriptable,
                args: Array<out Any>
            ): Any {
                val key = args.firstOrNull()?.toString() ?: return ""
                return try {
                    val json = if (storageFile.exists()) JSONObject(storageFile.readText()) else JSONObject()
                    json.optString(key, "")
                } catch (_: Throwable) {
                    ""
                }
            }
        })
        ScriptableObject.putProperty(scope, "storageSet", object : org.mozilla.javascript.BaseFunction() {
            override fun call(
                cx: RhinoContext,
                scope: Scriptable,
                thisObj: Scriptable,
                args: Array<out Any>
            ): Any {
                val key = args.getOrNull(0)?.toString() ?: return false
                val value = args.getOrNull(1)?.toString() ?: ""
                return try {
                    val json = if (storageFile.exists()) JSONObject(storageFile.readText()) else JSONObject()
                    json.put(key, value)
                    storageFile.writeText(json.toString())
                    true
                } catch (_: Throwable) {
                    false
                }
            }
        })
        ScriptableObject.putProperty(scope, "baseUrl", entry.site.orEmpty())
        ScriptableObject.putProperty(scope, "sourceId", entry.id)
    }

    private fun parseSearch(json: String): SearchPage {
        return try {
            val trimmed = json.trim()
            val arr = when {
                trimmed.startsWith("[") -> JSONArray(trimmed)
                else -> {
                    val o = JSONObject(trimmed)
                    o.optJSONArray("novels") ?: o.optJSONArray("results") ?: o.optJSONArray("items")
                        ?: JSONArray()
                }
            }
            val items = ArrayList<SeriesMeta>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val title = o.optString("title").ifBlank { o.optString("name") }
                val url = o.optString("url").ifBlank { o.optString("novelUrl") }
                if (title.isBlank() || url.isBlank()) continue
                items.add(
                    SeriesMeta(
                        url = url,
                        title = title,
                        coverUrl = o.optString("cover").ifBlank { o.optString("coverUrl") }.takeIf { it.isNotBlank() },
                        author = o.optString("author").takeIf { it.isNotBlank() },
                        kind = MediaKind.NOVEL,
                        sourceId = entry.id
                    )
                )
            }
            val hasNext = try {
                JSONObject(trimmed).optBoolean("hasNextPage", items.size >= 20)
            } catch (_: Exception) {
                items.size >= 20
            }
            SearchPage(items, hasNext)
        } catch (_: Exception) {
            SearchPage(emptyList(), false)
        }
    }

    private fun parseDetails(json: String, seriesUrl: String): SeriesMeta {
        return try {
            val o = JSONObject(json)
            SeriesMeta(
                url = o.optString("url").ifBlank { seriesUrl },
                title = o.optString("title").ifBlank { o.optString("name", "Unknown") },
                author = o.optString("author").takeIf { it.isNotBlank() },
                description = o.optString("summary").ifBlank { o.optString("description") }.takeIf { it.isNotBlank() },
                coverUrl = o.optString("cover").ifBlank { o.optString("coverUrl") }.takeIf { it.isNotBlank() },
                genres = o.optJSONArray("genres")?.let { a ->
                    (0 until a.length()).mapNotNull { a.optString(it).takeIf { s -> s.isNotBlank() } }
                }.orEmpty(),
                status = o.optString("status", "unknown"),
                kind = MediaKind.NOVEL,
                sourceId = entry.id
            )
        } catch (_: Exception) {
            SeriesMeta(url = seriesUrl, title = "Unknown", kind = MediaKind.NOVEL, sourceId = entry.id)
        }
    }

    private fun parseChapters(json: String): List<ChapterMeta> {
        return try {
            val trimmed = json.trim()
            val arr = when {
                trimmed.startsWith("[") -> JSONArray(trimmed)
                else -> {
                    val o = JSONObject(trimmed)
                    o.optJSONArray("chapters") ?: JSONArray()
                }
            }
            val out = ArrayList<ChapterMeta>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name").ifBlank { o.optString("title") }.ifBlank { "Chapter ${i + 1}" }
                val url = o.optString("url").ifBlank { o.optString("chapterUrl") }
                if (url.isBlank()) continue
                val num = o.optDouble("chapterNumber", (i + 1).toDouble()).toFloat()
                out.add(ChapterMeta(url = url, name = name, number = num, sourceId = entry.id))
            }
            out.sortedBy { it.number }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseContent(json: String): ContentPayload {
        return try {
            val trimmed = json.trim()
            if (trimmed.startsWith("{")) {
                val o = JSONObject(trimmed)
                val title = o.optString("title").takeIf { it.isNotBlank() }
                val text = o.optString("content").ifBlank {
                    o.optString("text").ifBlank {
                        o.optJSONArray("paragraphs")?.let { a ->
                            (0 until a.length()).joinToString("\n\n") { a.optString(it) }
                        }.orEmpty()
                    }
                }
                val blocks = text.split(Regex("(?<=[.!?…])\\s+"))
                    .chunked(5)
                    .mapIndexed { i, c -> TextBlock(i, c.joinToString(" ")) }
                    .filter { it.text.isNotBlank() }
                ContentPayload.Text(title, blocks)
            } else {
                val blocks = trimmed.split(Regex("(?<=[.!?…])\\s+"))
                    .chunked(5)
                    .mapIndexed { i, c -> TextBlock(i, c.joinToString(" ")) }
                ContentPayload.Text(null, blocks)
            }
        } catch (_: Exception) {
            ContentPayload.Text(null, listOf(TextBlock(0, json)))
        }
    }

    companion object {
        fun tryCreate(context: Context, entry: IndexEntry): LnReaderSourceAdapter? {
            val runtime = LnReaderJsRuntime(context)
            val file = runtime.pluginPath(entry.id)
            if (!file.exists() || file.length() < 20) {
                // Try download plugin if apkUrl points to JS
                val url = entry.apkUrl ?: return null
                if (!url.endsWith(".js") && !url.contains("plugin")) return null
                return try {
                    val client = OkHttpClient()
                    val body = client.newCall(Request.Builder().url(url).build()).execute()
                        .use { it.body?.string() }
                    if (body.isNullOrBlank() || body.length < 20) return null
                    runtime.savePlugin(entry.id, body)
                    LnReaderSourceAdapter(context, entry, body)
                } catch (_: Throwable) {
                    null
                }
            }
            return try {
                LnReaderSourceAdapter(context, entry, file.readText())
            } catch (_: Throwable) {
                null
            }
        }
    }
}
