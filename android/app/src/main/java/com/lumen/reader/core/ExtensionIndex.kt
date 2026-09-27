package com.lumen.reader.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class IndexEntry(
    val id: String,
    val name: String,
    val version: String,
    val lang: String,
    val kind: MediaKind,
    val nsfw: Boolean = false,
    val iconUrl: String? = null,
    val pkg: String? = null,
    val apkUrl: String? = null,
    val site: String? = null,
    val repoId: String,
    val versionCode: Long = 0L
)

object ExtensionIndexFetcher {
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val KEIYOUSHI =
        "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.json"
    private const val LNREADER =
        "https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v3.0.0/.dist/plugins.min.json"

    suspend fun fetchMangaIndex(): List<IndexEntry> = withContext(Dispatchers.IO) {
        val body = httpGet(KEIYOUSHI) ?: return@withContext emptyList()
        parseKeiyoushi(body)
    }

    suspend fun fetchNovelIndex(): List<IndexEntry> = withContext(Dispatchers.IO) {
        val body = httpGet(LNREADER) ?: return@withContext emptyList()
        parseLnReader(body)
    }

    suspend fun fetchAll(): List<IndexEntry> = withContext(Dispatchers.IO) {
        val manga = runCatching { fetchMangaIndex() }.getOrDefault(emptyList())
        val novel = runCatching { fetchNovelIndex() }.getOrDefault(emptyList())
        manga + novel
    }

    private fun httpGet(url: String): String? {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "LumenReader/1.5")
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            return resp.body?.string()
        }
    }

    private fun parseKeiyoushi(json: String): List<IndexEntry> {
        val root = JSONObject(json)
        val arr: JSONArray = when {
            root.has("extensionList") -> {
                val el = root.getJSONObject("extensionList")
                el.optJSONArray("extensions") ?: JSONArray()
            }
            root.has("extensions") -> root.getJSONArray("extensions")
            else -> {
                return try {
                    parseKeiyoushiLegacy(JSONArray(json))
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }
        val out = ArrayList<IndexEntry>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("name")
            if (name.isBlank()) continue
            val pkg = o.optString("packageName").ifBlank { o.optString("pkg") }
            if (pkg.isBlank()) continue
            val version = o.optString("versionName").ifBlank { o.optString("version", "1.0") }
            val code = o.optLong("versionCode", 0L)
            val res = o.optJSONObject("resources")
            val apkUrl = res?.optString("apkUrl")?.takeIf { it.isNotBlank() }
                ?: o.optString("apk").takeIf { it.isNotBlank() }
            val iconUrl = res?.optString("iconUrl")?.takeIf { it.isNotBlank() }
            val sources = o.optJSONArray("sources")
            var lang = "all"
            var site: String? = null
            if (sources != null && sources.length() > 0) {
                val s0 = sources.optJSONObject(0)
                if (s0 != null) {
                    lang = s0.optString("language").ifBlank { s0.optString("lang", "all") }
                    site = s0.optString("homeUrl").takeIf { it.isNotBlank() }
                        ?: s0.optString("baseUrl").takeIf { it.isNotBlank() }
                }
            }
            val nsfw = when {
                o.optString("contentWarning").contains("NSFW", true) -> true
                o.optBoolean("nsfw", false) -> true
                else -> false
            }
            out.add(
                IndexEntry(
                    id = pkg,
                    name = name,
                    version = version,
                    lang = lang,
                    kind = MediaKind.MANGA,
                    nsfw = nsfw,
                    iconUrl = iconUrl,
                    pkg = pkg,
                    apkUrl = apkUrl,
                    site = site,
                    repoId = "keiyoushi",
                    versionCode = code
                )
            )
        }
        return out
    }

    private fun parseKeiyoushiLegacy(arr: JSONArray): List<IndexEntry> {
        val out = ArrayList<IndexEntry>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("name")
            if (name.isBlank()) continue
            val pkg = o.optString("pkg").ifBlank { o.optString("id") }
            out.add(
                IndexEntry(
                    id = pkg.ifBlank { "keiyoushi-$name" },
                    name = name,
                    version = o.optString("version").ifBlank { o.optInt("code").toString() },
                    lang = o.optString("lang", "all"),
                    kind = MediaKind.MANGA,
                    nsfw = o.optBoolean("nsfw", false) || o.optInt("nsfw", 0) == 1,
                    pkg = pkg,
                    apkUrl = o.optString("apk").takeIf { it.isNotBlank() },
                    repoId = "keiyoushi"
                )
            )
        }
        return out
    }

    private fun parseLnReader(json: String): List<IndexEntry> {
        val trimmed = json.trim()
        val arr = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            else -> {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("plugins") ?: obj.optJSONArray("data") ?: JSONArray()
            }
        }
        val out = ArrayList<IndexEntry>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("name").ifBlank { o.optString("id") }
            if (name.isBlank()) continue
            val id = o.optString("id").ifBlank { "lnreader-$name" }
            out.add(
                IndexEntry(
                    id = id,
                    name = name,
                    version = o.optString("version", "1.0.0"),
                    lang = o.optString("lang", o.optString("language", "en")),
                    kind = MediaKind.NOVEL,
                    nsfw = o.optBoolean("nsfw", false),
                    iconUrl = o.optString("iconUrl").takeIf { it.isNotBlank() },
                    site = o.optString("site").takeIf { it.isNotBlank() }
                        ?: o.optString("url").takeIf { it.isNotBlank() },
                    repoId = "lnreader"
                )
            )
        }
        return out
    }
}
