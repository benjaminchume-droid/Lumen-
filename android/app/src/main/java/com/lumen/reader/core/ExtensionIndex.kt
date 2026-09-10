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
    val repoId: String
)

object ExtensionIndexFetcher {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val KEIYOUSHI =
        "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json"
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
        val req = Request.Builder().url(url).header("User-Agent", "LumenReader/1.0").build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            return resp.body?.string()
        }
    }

    private fun parseKeiyoushi(json: String): List<IndexEntry> {
        val arr = JSONArray(json)
        val out = ArrayList<IndexEntry>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("name")
            if (name.isBlank()) continue
            val pkg = o.optString("pkg").ifBlank { o.optString("id") }
            val version = o.optString("version").ifBlank { o.optInt("code").toString() }
            val lang = o.optString("lang", "all")
            val nsfw = o.optBoolean("nsfw", false)
            val apk = o.optString("apk").takeIf { it.isNotBlank() }
            val icon = o.optString("iconUrl").takeIf { it.isNotBlank() }
                ?: o.optString("icon").takeIf { it.isNotBlank() }
            val id = pkg.ifBlank { "keiyoushi-$name" }
            out.add(
                IndexEntry(
                    id = id,
                    name = name,
                    version = version,
                    lang = lang,
                    kind = MediaKind.MANGA,
                    nsfw = nsfw,
                    iconUrl = icon,
                    pkg = pkg,
                    apkUrl = apk,
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
            val version = o.optString("version", "1.0.0")
            val lang = o.optString("lang", o.optString("language", "en"))
            val site = o.optString("site").takeIf { it.isNotBlank() }
                ?: o.optString("url").takeIf { it.isNotBlank() }
            val icon = o.optString("iconUrl").takeIf { it.isNotBlank() }
                ?: o.optString("icon").takeIf { it.isNotBlank() }
            out.add(
                IndexEntry(
                    id = id,
                    name = name,
                    version = version,
                    lang = lang,
                    kind = MediaKind.NOVEL,
                    nsfw = o.optBoolean("nsfw", false),
                    iconUrl = icon,
                    site = site,
                    repoId = "lnreader"
                )
            )
        }
        return out
    }
}
