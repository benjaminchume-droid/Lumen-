package com.lumen.reader.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists installed extension ids + metadata (Mihon-style: index first, install on demand).
 */
class SourceStore(context: Context) {
    private val prefs = context.getSharedPreferences("lumen_sources", Context.MODE_PRIVATE)

    fun installedIds(): Set<String> =
        prefs.getStringSet(KEY_INSTALLED, emptySet()) ?: emptySet()

    fun isInstalled(id: String): Boolean = installedIds().contains(id)

    fun install(entry: IndexEntry) {
        val ids = installedIds().toMutableSet()
        ids.add(entry.id)
        prefs.edit()
            .putStringSet(KEY_INSTALLED, ids)
            .putString(metaKey(entry.id), entry.toJson())
            .apply()
    }

    fun uninstall(id: String) {
        val ids = installedIds().toMutableSet()
        ids.remove(id)
        prefs.edit()
            .putStringSet(KEY_INSTALLED, ids)
            .remove(metaKey(id))
            .apply()
    }

    fun getInstalled(): List<IndexEntry> =
        installedIds().mapNotNull { id ->
            prefs.getString(metaKey(id), null)?.let { parseJson(it) }
        }.sortedBy { it.name.lowercase() }

    private fun metaKey(id: String) = "meta_$id"

    private fun IndexEntry.toJson(): String {
        val o = JSONObject()
        o.put("id", id)
        o.put("name", name)
        o.put("version", version)
        o.put("lang", lang)
        o.put("kind", kind.name)
        o.put("nsfw", nsfw)
        o.put("iconUrl", iconUrl)
        o.put("pkg", pkg)
        o.put("apkUrl", apkUrl)
        o.put("site", site)
        o.put("repoId", repoId)
        return o.toString()
    }

    private fun parseJson(s: String): IndexEntry? = try {
        val o = JSONObject(s)
        IndexEntry(
            id = o.getString("id"),
            name = o.getString("name"),
            version = o.optString("version", "1"),
            lang = o.optString("lang", "all"),
            kind = runCatching { MediaKind.valueOf(o.optString("kind", "MANGA")) }
                .getOrDefault(MediaKind.MANGA),
            nsfw = o.optBoolean("nsfw", false),
            iconUrl = o.optString("iconUrl").takeIf { it.isNotBlank() },
            pkg = o.optString("pkg").takeIf { it.isNotBlank() },
            apkUrl = o.optString("apkUrl").takeIf { it.isNotBlank() },
            site = o.optString("site").takeIf { it.isNotBlank() },
            repoId = o.optString("repoId", "")
        )
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val KEY_INSTALLED = "installed_ids"
    }
}
