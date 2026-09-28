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

    @Volatile
    var userId: String? = null
        private set

    @Volatile
    var lastError: String? = null
        private set

    fun clearToken() {
        accessToken = null
        userId = null
    }

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
        } catch (_: Exception) {
        }
    }

    suspend fun signUp(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        lastError = null
        try {
            val payload = JSONObject().put("email", email).put("password", password).toString()
            val req = Request.Builder().url("$URL/auth/v1/signup")
                .header("apikey", ANON_KEY).header("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMedia)).build()
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
            val req = Request.Builder().url("$URL/auth/v1/token?grant_type=password")
                .header("apikey", ANON_KEY).header("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMedia)).build()
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
            val req = Request.Builder().url("$URL/auth/v1/otp")
                .header("apikey", ANON_KEY).header("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMedia)).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful || resp.code == 429) true
                else { lastError = parseAuthError(body) ?: "Could not send code (${resp.code})"; false }
            }
        } catch (e: Exception) { lastError = e.message ?: "Network error"; false }
    }

    suspend fun verifyOtp(email: String, token: String): Boolean = withContext(Dispatchers.IO) {
        lastError = null
        try {
            val payload = JSONObject().put("email", email).put("token", token).put("type", "email").toString()
            val req = Request.Builder().url("$URL/auth/v1/verify")
                .header("apikey", ANON_KEY).header("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMedia)).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) { captureSession(body); true }
                else { lastError = parseAuthError(body) ?: "Invalid or expired code"; false }
            }
        } catch (e: Exception) { lastError = e.message ?: "Network error"; false }
    }

    suspend fun sendRecoveryOtp(email: String): Boolean = withContext(Dispatchers.IO) {
        lastError = null
        try {
            val payload = JSONObject().put("email", email).put("create_user", false).toString()
            val req = Request.Builder().url("$URL/auth/v1/otp")
                .header("apikey", ANON_KEY).header("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMedia)).build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful || resp.code == 429) true
                else { lastError = parseAuthError(resp.body?.string().orEmpty()) ?: "Could not send reset code"; false }
            }
        } catch (e: Exception) { lastError = e.message ?: "Network error"; false }
    }

    suspend fun verifyRecoveryOtp(email: String, token: String): Boolean = withContext(Dispatchers.IO) {
        lastError = null
        try {
            val payload = JSONObject().put("email", email).put("token", token).put("type", "recovery").toString()
            val req = Request.Builder().url("$URL/auth/v1/verify")
                .header("apikey", ANON_KEY).header("Content-Type", "application/json")
                .post(payload.toRequestBody(jsonMedia)).build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) { captureSession(body); true } else verifyOtp(email, token)
            }
        } catch (e: Exception) { lastError = e.message ?: "Network error"; false }
    }

    suspend fun updatePassword(newPassword: String): Boolean = withContext(Dispatchers.IO) {
        lastError = null
        val token = accessToken
        if (token.isNullOrBlank()) { lastError = "Session expired — verify code again"; return@withContext false }
        try {
            val payload = JSONObject().put("password", newPassword).toString()
            val req = Request.Builder().url("$URL/auth/v1/user")
                .header("apikey", ANON_KEY).header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .put(payload.toRequestBody(jsonMedia)).build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) true
                else { lastError = parseAuthError(resp.body?.string().orEmpty()) ?: "Could not update password"; false }
            }
        } catch (e: Exception) { lastError = e.message ?: "Network error"; false }
    }

    suspend fun isUsernameAvailable(username: String): Boolean = withContext(Dispatchers.IO) {
        lastError = null
        try {
            val q = java.net.URLEncoder.encode(username, "UTF-8")
            val req = Request.Builder()
                .url("$URL/rest/v1/profiles?select=id&username=ilike.$q&limit=1")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer ${accessToken ?: ANON_KEY}")
                .header("Accept", "application/json").get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) { lastError = "Could not check username"; return@withContext false }
                val arr = try { JSONArray(body) } catch (_: Exception) { JSONArray() }
                arr.length() == 0
            }
        } catch (e: Exception) { lastError = e.message; false }
    }

    suspend fun upsertProfile(username: String, email: String): Boolean = withContext(Dispatchers.IO) {
        lastError = null
        val token = accessToken
        val uid = userId
        if (token.isNullOrBlank() || uid.isNullOrBlank()) { lastError = "Not authenticated"; return@withContext false }
        try {
            val payload = JSONObject().put("id", uid).put("username", username)
                .put("display_name", username).put("email", email)
                .put("updated_at", java.time.Instant.now().toString()).toString()
            val req = Request.Builder().url("$URL/rest/v1/profiles")
                .header("apikey", ANON_KEY).header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(payload.toRequestBody(jsonMedia)).build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful || resp.code in 200..299) true
                else { lastError = parseAuthError(resp.body?.string().orEmpty()) ?: "Profile save failed (${resp.code})"; false }
            }
        } catch (e: Exception) { lastError = e.message ?: "Network error"; false }
    }

    suspend fun saveInterests(genres: List<String>): Boolean = withContext(Dispatchers.IO) {
        lastError = null
        val token = accessToken
        val uid = userId
        if (token.isNullOrBlank() || uid.isNullOrBlank()) return@withContext false
        try {
            val arr = JSONArray(); genres.forEach { arr.put(it) }
            val payload = JSONObject().put("interests", arr)
                .put("updated_at", java.time.Instant.now().toString()).toString()
            val req = Request.Builder().url("$URL/rest/v1/profiles?id=eq.$uid")
                .header("apikey", ANON_KEY).header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json").header("Prefer", "return=minimal")
                .method("PATCH", payload.toRequestBody(jsonMedia)).build()
            client.newCall(req).execute().use { it.isSuccessful || it.code in 200..299 }
        } catch (_: Exception) { false }
    }

    suspend fun saveInterests(interests: String): Boolean =
        saveInterests(interests.split(',').map { it.trim() }.filter { it.isNotEmpty() })

    private fun parseAuthError(body: String): String? {
        return try {
            val j = JSONObject(body)
            j.optString("error_description").ifBlank {
                j.optString("msg").ifBlank {
                    j.optString("message").ifBlank {
                        j.optJSONObject("error")?.optString("message").orEmpty()
                    }
                }
            }.ifBlank { null }
        } catch (_: Exception) { null }
    }
}
