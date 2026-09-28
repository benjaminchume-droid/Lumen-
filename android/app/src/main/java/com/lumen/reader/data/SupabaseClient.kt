package com.lumen.reader.data

import com.lumen.reader.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SupabaseClient {

    private val URL: String get() = BuildConfig.SUPABASE_URL
    private val ANON_KEY: String get() = BuildConfig.SUPABASE_ANON_KEY

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json".toMediaType()

    @Volatile
    var accessToken: String? = null
        private set

    fun clearToken() { accessToken = null }

    data class Health(val ok: Boolean, val message: String, val seriesCount: Int = 0)

    suspend fun checkHealth(): Health = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$URL/rest/v1/series?select=id&limit=1")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer $ANON_KEY")
                .header("Accept", "application/json")
                .get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val count = try { JSONArray(body).length() } catch (_: Exception) { 0 }
                    Health(true, "Connected to Lumen Supabase", count)
                } else if (resp.code in listOf(200, 206, 404, 406) || body.contains("PGRST")) {
                    Health(true, "Supabase reachable (schema pending or empty)", 0)
                } else Health(false, "HTTP ${resp.code}: ${body.take(120)}")
            }
        } catch (e: Exception) {
            Health(false, e.message ?: "Network error")
        }
    }

    suspend fun signUp(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().put("email", email).put("password", password).toString()
            val req = Request.Builder()
                .url("$URL/auth/v1/signup")
                .header("apikey", ANON_KEY)
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMedia)).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val tok = JSONObject(body).optString("access_token").ifBlank {
                        JSONObject(body).optJSONObject("session")?.optString("access_token").orEmpty()
                    }
                    if (tok.isNotBlank()) accessToken = tok
                    true
                } else false
            }
        } catch (_: Exception) { false }
    }

    suspend fun signIn(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().put("email", email).put("password", password).toString()
            val req = Request.Builder()
                .url("$URL/auth/v1/token?grant_type=password")
                .header("apikey", ANON_KEY)
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMedia)).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val tok = JSONObject(body).optString("access_token")
                    if (tok.isNotBlank()) accessToken = tok
                    true
                } else false
            }
        } catch (_: Exception) { false }
    }

    suspend fun sendOtp(email: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().put("email", email).put("create_user", true).toString()
            val req = Request.Builder()
                .url("$URL/auth/v1/otp")
                .header("apikey", ANON_KEY)
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMedia)).build()
            client.newCall(req).execute().use { resp -> resp.isSuccessful || resp.code == 429 }
        } catch (_: Exception) { false }
    }

    suspend fun verifyOtp(email: String, token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().put("email", email).put("token", token).put("type", "email").toString()
            val req = Request.Builder()
                .url("$URL/auth/v1/verify")
                .header("apikey", ANON_KEY)
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMedia)).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val tok = JSONObject(body).optString("access_token")
                    if (tok.isNotBlank()) accessToken = tok
                    true
                } else false
            }
        } catch (_: Exception) { false }
    }

    suspend fun saveInterests(interests: String): Boolean = withContext(Dispatchers.IO) {
        val token = accessToken ?: ANON_KEY
        try {
            val payload = JSONObject().put("interests", interests).put("updated_at", java.time.Instant.now().toString()).toString()
            val req = Request.Builder()
                .url("$URL/rest/v1/profiles")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates")
                .post(payload.toRequestBody(jsonMedia)).build()
            client.newCall(req).execute().use { it.isSuccessful || it.code in 200..299 }
        } catch (_: Exception) { false }
    }

    suspend fun listSeries(limit: Int = 20): List<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$URL/rest/v1/series?select=*&limit=$limit&order=updated_at.desc")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer $ANON_KEY")
                .header("Accept", "application/json")
                .get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val arr = JSONArray(resp.body?.string().orEmpty())
                (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
            }
        } catch (_: Exception) { emptyList() }
    }
}
