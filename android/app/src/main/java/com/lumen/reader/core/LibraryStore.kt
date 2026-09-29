package com.lumen.reader.core

import android.content.Context
import com.lumen.reader.ui.CatalogSeries
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent library + history. Survives app restarts and version upgrades
 * (same shared_prefs name; MODE_PRIVATE).
 */
class LibraryStore(context: Context) {
    private val prefs = context.getSharedPreferences("lumen_library_v1", Context.MODE_PRIVATE)

    fun loadLibrary(): List<CatalogSeries> = readList("library_json")
    fun loadHistory(): List<CatalogSeries> = readList("history_json")

    fun saveLibrary(items: List<CatalogSeries>) {
        prefs.edit().putString("library_json", toJson(items)).apply()
    }

    fun saveHistory(items: List<CatalogSeries>) {
        prefs.edit().putString("history_json", toJson(items.take(80))).apply()
    }

    fun addToLibrary(series: CatalogSeries) {
        val cur = loadLibrary().filter { it.id != series.id }.toMutableList()
        cur.add(0, series)
        saveLibrary(cur)
    }

    fun removeFromLibrary(id: String) {
        saveLibrary(loadLibrary().filter { it.id != id })
    }

    fun isInLibrary(id: String): Boolean = loadLibrary().any { it.id == id }

    fun pushHistory(series: CatalogSeries) {
        val cur = listOf(series) + loadHistory().filter { it.id != series.id }
        saveHistory(cur)
    }

    private fun toJson(items: List<CatalogSeries>): String {
        val arr = JSONArray()
        items.forEach { s ->
            arr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("title", s.title)
                    .put("author", s.author)
                    .put("sourceName", s.sourceName)
                    .put("kind", s.kind)
                    .put("description", s.description)
                    .put("genres", JSONArray(s.genres))
                    .put("status", s.status)
                    .put("coverHint", s.coverHint)
                    .put("coverUrl", s.coverUrl ?: JSONObject.NULL)
                    .put("seriesUrl", s.seriesUrl ?: JSONObject.NULL)
                    .put("sourceId", s.sourceId)
            )
        }
        return arr.toString()
    }

    private fun readList(key: String): List<CatalogSeries> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val genres = mutableListOf<String>()
                val g = o.optJSONArray("genres")
                if (g != null) for (j in 0 until g.length()) genres.add(g.optString(j))
                CatalogSeries(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    author = o.optString("author"),
                    sourceName = o.optString("sourceName"),
                    kind = o.optString("kind", "manga"),
                    description = o.optString("description"),
                    genres = genres,
                    status = o.optString("status", "Ongoing"),
                    chapters = emptyList(),
                    coverHint = o.optLong("coverHint", 0xFF1E2A3A),
                    coverUrl = o.optString("coverUrl").takeIf { it.isNotBlank() && it != "null" },
                    seriesUrl = o.optString("seriesUrl").takeIf { it.isNotBlank() && it != "null" },
                    sourceId = o.optString("sourceId")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
