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

    @Volatile var accessToken: String? = null; private set
    @Volatile var userId: String? = null; private set
    @Volatile var lastError: String? = null; private set

    fun clearToken() { accessToken = null; userId = null }

    private fun captureSession(body: String) {
        try {
            val json = JSONObject(body)
            val tok = json.optString("access_token").ifBlank {
                json.optJSONObject("session")?.optString("access_token").orEmpty()
            }
            if (tok.isNotBlank()) accessToken = tok
            val uid = json.optJSONObject("user")?.optString("id").orEmpty().ifBlank {
                json.optJSONObject("session")?.optJSONObject("user")?.optString("id").orEmpty()
            }
            if (uid.isNotBlank()) userId = uid
        } catch (_: Exception) {}
    }

    suspend fun signUp(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        lastError = null
        try {
            val payload = JSONObject().put("email", email).put("password", password).toString()
            val req = Request.Builder().url("$URL/auth/v1/signup").header("apikey", ANON_KEY)
                .header("Content-Type", "application/json").post(payload.toRequestBody(jsonMedia)).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) { captureSession(body); true }
                else { lastError = parseAuthError(body) ?: "Sign up failed (${resp.code})"; false }
            }
        } catch (e: Exception) { lastError = e.message ?: "Network error"; false }
    }

    suspend fun signIn(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        lastError = null
        try {
            val payload = JSONObject().put("email", email).put("password", password).toString()
            val req = Request.Builder().url("$URL/auth/v1/token?grant_type=password").header("apikey", ANON_KEY)
                .header("Content-Type", "application/json").post(payload.toRequestBody(jsonMedia)).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) { captureSession(body); true }
                else { lastError = parseAuthError(body) ?: "Invalid email or password"; false }
            }
        } catch (e: Exception) { lastError = e.message ?: "Network error"; false }
    }

    suspend fun sendOtp(email: String): Boolean = withContext(Dispatchers.IO) {
        lastError = null
        try {
            val payload = JSONObject().put("email", email).put("create_user", true).toString()
            val req = Request.Builder().url("$URL/auth/v1/otp").header("apikey", ANON_KEY)
                .header("Content-Type", "application/json").post(payload.toRequestBody(jsonMedia)).build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful || resp.code == 429) true
                else { lastError = parseAuthError(resp.body?.string().orEmpty()) ?: "Could not send code"; false }
            }
        } catch (e: Exception) { lastError = e.message ?: "Network error"; false }
    }

    suspend fun verifyOtp(email: String, token: String): Boolean = withContext(Dispatchers.IO) {
        lastError = null
        try {
            val payload = JSONObject().put("email", email).put("token", token).put("type", "email").toString()
            val req = Request.Builder().url("$URL/auth/v1/verify").header("apikey", ANON_KEY)
                .header("Content-Type", "application/json").post(payload.toRequestBody(jsonMedia)).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) { captureSession(body); true }
                else { lastError = parseAuthError(body) ?: "Invalid or expired code"; false }
            }
        } catch (e: Exception) { lastError = e.message ?: "Network error"; false }
    }

    suspend fun sendRecoveryOtp(email: String): Boolean = sendOtp(email)
    suspend fun verifyRecoveryOtp(email: String, token: String): Boolean = verifyOtp(email, token)

    suspend fun updatePassword(newPassword: String): Boolean = withContext(Dispatchers.IO) {
        lastError = null
        val token = accessToken ?: run { lastError = "Session expired"; return@withContext false }
        try {
            val payload = JSONObject().put("password", newPassword).toString()
            val req = Request.Builder().url("$URL/auth/v1/user").header("apikey", ANON_KEY)
                .header("Authorization", "Bearer $token").header("Content-Type", "application/json")
                .put(payload.toRequestBody(jsonMedia)).build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) { lastError = e.message; false }
    }

    suspend fun isUsernameAvailable(username: String): Boolean = withContext(Dispatchers.IO) {
        lastError = null
        try {
            val q = java.net.URLEncoder.encode(username, "UTF-8")
            val req = Request.Builder()
                .url("$URL/rest/v1/profiles?select=id&username=ilike.$q&limit=1")
                .header("apikey", ANON_KEY).header("Authorization", "Bearer ${accessToken ?: ANON_KEY}")
                .header("Accept", "application/json").get().build()
            client.newCall(req).execute().use { resp ->
                val arr = try { JSONArray(resp.body?.string().orEmpty()) } catch (_: Exception) { JSONArray() }
                arr.length() == 0
            }
        } catch (e: Exception) { lastError = e.message; false }
    }

    suspend fun upsertProfile(username: String, email: String): Boolean = withContext(Dispatchers.IO) {
        lastError = null
        val token = accessToken; val uid = userId
        if (token.isNullOrBlank() || uid.isNullOrBlank()) { lastError = "Not authenticated"; return@withContext false }
        try {
            val payload = JSONObject().put("id", uid).put("username", username)
                .put("display_name", username).put("email", email)
                .put("updated_at", java.time.Instant.now().toString()).toString()
            val req = Request.Builder().url("$URL/rest/v1/profiles").header("apikey", ANON_KEY)
                .header("Authorization", "Bearer $token").header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(payload.toRequestBody(jsonMedia)).build()
            client.newCall(req).execute().use { it.isSuccessful || it.code in 200..299 }
        } catch (e: Exception) { lastError = e.message; false }
    }

    suspend fun saveInterests(genres: List<String>): Boolean = withContext(Dispatchers.IO) {
        val token = accessToken; val uid = userId
        if (token.isNullOrBlank() || uid.isNullOrBlank()) return@withContext false
        try {
            val arr = JSONArray(); genres.forEach { arr.put(it) }
            val payload = JSONObject().put("interests", arr)
                .put("updated_at", java.time.Instant.now().toString()).toString()
            val req = Request.Builder().url("$URL/rest/v1/profiles?id=eq.$uid").header("apikey", ANON_KEY)
                .header("Authorization", "Bearer $token").header("Content-Type", "application/json")
                .header("Prefer", "return=minimal").method("PATCH", payload.toRequestBody(jsonMedia)).build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) { false }
    }

    suspend fun saveInterests(interests: String): Boolean =
        saveInterests(interests.split(',').map { it.trim() }.filter { it.isNotEmpty() })

    suspend fun publishSeries(
        title: String, description: String, coverUrl: String?, contentType: String,
        genres: List<String>, firstChapterTitle: String?, firstChapterText: String?
    ): Boolean = withContext(Dispatchers.IO) {
        lastError = null
        val token = accessToken
        if (token.isNullOrBlank()) { lastError = "Not signed in"; return@withContext false }
        val safeChapterText = firstChapterText?.takeIf { it.isNotBlank() }
        val safeChapterTitle = firstChapterTitle ?: "Chapter 1"
        try {
            val payload = JSONObject().put("title", title).put("description", description)
                .put("cover_url", coverUrl ?: JSONObject.NULL)
                .put("content_type", contentType).put("origin_type", "lumen")
                .put("status", "ongoing").put("language", "en")
            val req = Request.Builder().url("$URL/rest/v1/series").header("apikey", ANON_KEY)
                .header("Authorization", "Bearer $token").header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .post(payload.toString().toRequestBody(jsonMedia)).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    lastError = parseAuthError(body) ?: "Publish failed (${resp.code})"
                    return@withContext false
                }
                val seriesId = try {
                    JSONArray(body).optJSONObject(0)?.optString("id").orEmpty()
                } catch (_: Exception) {
                    try { JSONObject(body).optString("id") } catch (_: Exception) { "" }
                }
                if (seriesId.isNotBlank() && safeChapterText != null) {
                    val text: String = safeChapterText
                    val chPayload = JSONObject().put("series_id", seriesId).put("chapter_number", 1)
                        .put("title", safeChapterTitle)
                        .put("content_type", "text").put("status", "published")
                    val chReq = Request.Builder().url("$URL/rest/v1/chapters").header("apikey", ANON_KEY)
                        .header("Authorization", "Bearer $token").header("Content-Type", "application/json")
                        .header("Prefer", "return=representation")
                        .post(chPayload.toString().toRequestBody(jsonMedia)).build()
                    client.newCall(chReq).execute().use { chResp ->
                        val chBody = chResp.body?.string().orEmpty()
                        val chId = try { JSONArray(chBody).optJSONObject(0)?.optString("id") } catch (_: Exception) { null }
                        if (!chId.isNullOrBlank()) {
                            val contentPayload = JSONObject().put("chapter_id", chId)
                                .put("content", text)
                                .put("word_count", text.split(Regex("\\s+")).size)
                            val cReq = Request.Builder().url("$URL/rest/v1/chapter_content").header("apikey", ANON_KEY)
                                .header("Authorization", "Bearer $token").header("Content-Type", "application/json")
                                .header("Prefer", "return=minimal")
                                .post(contentPayload.toString().toRequestBody(jsonMedia)).build()
                            client.newCall(cReq).execute().use { }
                        }
                    }
                }
                true
            }
        } catch (e: Exception) { lastError = e.message ?: "Network error"; false }
    }

    data class LumenSeries(val id: String, val title: String, val description: String, val coverUrl: String?, val contentType: String)

    suspend fun fetchLumenFeed(limit: Int = 30): List<LumenSeries> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$URL/rest/v1/series?select=id,title,description,cover_url,content_type&origin_type=eq.lumen&order=created_at.desc&limit=$limit")
                .header("apikey", ANON_KEY).header("Authorization", "Bearer ${accessToken ?: ANON_KEY}")
                .header("Accept", "application/json").get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val arr = JSONArray(resp.body?.string().orEmpty())
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    LumenSeries(
                        o.optString("id"),
                        o.optString("title"),
                        o.optString("description"),
                        o.optString("cover_url").takeIf { it.isNotBlank() },
                        o.optString("content_type", "novel")
                    )
                }
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun refreshProfileUsername(): String? = withContext(Dispatchers.IO) {
        val token = accessToken ?: return@withContext null
        val uid = userId ?: return@withContext null
        try {
            val req = Request.Builder()
                .url("$URL/rest/v1/profiles?select=username&id=eq.$uid&limit=1")
                .header("apikey", ANON_KEY).header("Authorization", "Bearer $token")
                .header("Accept", "application/json").get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val uname = JSONArray(resp.body?.string().orEmpty()).optJSONObject(0)?.optString("username")
                if (uname.isNullOrBlank()) null else uname
            }
        } catch (_: Exception) { null }
    }

    private fun parseAuthError(body: String): String? {
        return try {
            val j = JSONObject(body)
            val candidates = listOf(
                j.optString("error_description"),
                j.optString("msg"),
                j.optString("message"),
                j.optJSONObject("error")?.optString("message").orEmpty()
            )
            candidates.firstOrNull { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
