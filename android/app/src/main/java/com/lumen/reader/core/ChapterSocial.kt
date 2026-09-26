package com.lumen.reader.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Social reactions keyed by external lumen series id + chapter id.
 * Works offline locally; can later sync to Supabase by the same keys.
 */
enum class ChapterVote { NONE, LIKE, DISLIKE }

data class ChapterComment(
    val id: String,
    val author: String,
    val body: String,
    val createdAt: Long
)

class ChapterSocialStore(context: Context) {
    private val prefs = context.getSharedPreferences("lumen_chapter_social", Context.MODE_PRIVATE)

    private fun key(seriesId: String, chapterId: String) = "$seriesId::$chapterId"

    fun getVote(seriesId: String, chapterId: String): ChapterVote {
        val v = prefs.getString("vote:${key(seriesId, chapterId)}", "NONE") ?: "NONE"
        return runCatching { ChapterVote.valueOf(v) }.getOrDefault(ChapterVote.NONE)
    }

    fun setVote(seriesId: String, chapterId: String, vote: ChapterVote) {
        val k = key(seriesId, chapterId)
        val current = getVote(seriesId, chapterId)
        val next = if (current == vote) ChapterVote.NONE else vote
        prefs.edit().putString("vote:$k", next.name).apply()

        // rough counters
        val likes = prefs.getInt("likes:$k", 0)
        val dislikes = prefs.getInt("dislikes:$k", 0)
        var l = likes
        var d = dislikes
        if (current == ChapterVote.LIKE) l = (l - 1).coerceAtLeast(0)
        if (current == ChapterVote.DISLIKE) d = (d - 1).coerceAtLeast(0)
        if (next == ChapterVote.LIKE) l++
        if (next == ChapterVote.DISLIKE) d++
        prefs.edit().putInt("likes:$k", l).putInt("dislikes:$k", d).apply()
    }

    fun likeCount(seriesId: String, chapterId: String) =
        prefs.getInt("likes:${key(seriesId, chapterId)}", 0)

    fun dislikeCount(seriesId: String, chapterId: String) =
        prefs.getInt("dislikes:${key(seriesId, chapterId)}", 0)

    fun getComments(seriesId: String, chapterId: String): List<ChapterComment> {
        val raw = prefs.getString("comments:${key(seriesId, chapterId)}", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ChapterComment(
                    id = o.optString("id"),
                    author = o.optString("author", "Reader"),
                    body = o.optString("body"),
                    createdAt = o.optLong("createdAt", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun addComment(seriesId: String, chapterId: String, author: String, body: String) {
        val list = getComments(seriesId, chapterId).toMutableList()
        list.add(
            0,
            ChapterComment(
                id = "c_${System.currentTimeMillis()}",
                author = author.ifBlank { "Reader" },
                body = body.trim(),
                createdAt = System.currentTimeMillis()
            )
        )
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("author", c.author)
                    .put("body", c.body)
                    .put("createdAt", c.createdAt)
            )
        }
        prefs.edit().putString("comments:${key(seriesId, chapterId)}", arr.toString()).apply()
    }
}
