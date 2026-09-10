package com.lumen.reader.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Lightweight Supabase PostgREST client for Lumen project ryoewtikgwmyejrpjgnw.
 */
object SupabaseClient {
    const val URL = "https://ryoewtikgwmyejrpjgnw.supabase.co"
    const val ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJ5b2V3dGlrZ3dteWVqcnBqZ253Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY3MDg3MzYsImV4cCI6MjEwMjI4NDczNn0.KNa80UeXpIuPlVEt5sFIq4TyYtIc2ykWe-k24m7O7Pg"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json".toMediaType()

    data class Health(
        val ok: Boolean,
        val message: String,
        val seriesCount: Int = 0
    )

    suspend fun checkHealth(): Health = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$URL/rest/v1/series?select=id&limit=1")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer $ANON_KEY")
                .header("Accept", "application/json")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val count = try {
                        JSONArray(body).length()
                    } catch (_: Exception) {
                        0
                    }
                    Health(true, "Connected to Lumen Supabase", count)
                } else if (resp.code in listOf(200, 206, 404, 406) || body.contains("PGRST")) {
                    Health(true, "Supabase reachable (schema pending or empty)", 0)
                } else {
                    Health(false, "HTTP ${resp.code}: ${body.take(120)}")
                }
            }
        } catch (e: Exception) {
            Health(false, e.message ?: "Network error")
        }
    }

    suspend fun listSeries(limit: Int = 20): List<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$URL/rest/v1/series?select=*&limit=$limit&order=updated_at.desc")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer $ANON_KEY")
                .header("Accept", "application/json")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val arr = JSONArray(resp.body?.string().orEmpty())
                (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
