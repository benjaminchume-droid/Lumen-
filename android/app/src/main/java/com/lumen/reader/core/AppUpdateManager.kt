package com.lumen.reader.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.lumen.reader.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

enum class UpdatePhase { IDLE, CHECKING, AVAILABLE, DOWNLOADING, READY, INSTALLING, UP_TO_DATE, FAILED }

data class UpdateState(
    val phase: UpdatePhase = UpdatePhase.IDLE,
    val message: String = "",
    val remoteTag: String = "",
    val remoteVersionCode: Long = 0L,
    val apkUrl: String? = null,
    val bytesDone: Long = 0L,
    val bytesTotal: Long = 0L
) {
    val percent: Int
        get() = when {
            bytesTotal > 0 -> ((bytesDone * 100) / bytesTotal).toInt().coerceIn(0, 100)
            phase == UpdatePhase.READY || phase == UpdatePhase.UP_TO_DATE -> 100
            else -> 0
        }
}

class AppUpdateManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val prefs = context.getSharedPreferences("lumen_app_update", Context.MODE_PRIVATE)
    private val updateDir = File(context.cacheDir, "app_updates").also { it.mkdirs() }

    private val _state = MutableStateFlow(UpdateState())
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private fun localVersionName(): String = BuildConfig.VERSION_NAME
    private fun localVersionCode(): Long = BuildConfig.VERSION_CODE.toLong()

    private fun parseVersionCode(tag: String): Long {
        val digits = Regex("""(\d+)(?:\.(\d+))?(?:\.(\d+))?""").find(tag)
        if (digits != null) {
            val major = digits.groupValues.getOrNull(1)?.toLongOrNull() ?: 0
            val minor = digits.groupValues.getOrNull(2)?.toLongOrNull() ?: 0
            val patch = digits.groupValues.getOrNull(3)?.toLongOrNull() ?: 0
            return major * 10000 + minor * 100 + patch
        }
        return 0L
    }

    suspend fun checkForUpdate(): UpdateState = withContext(Dispatchers.IO) {
        _state.value = UpdateState(phase = UpdatePhase.CHECKING, message = "Checking GitHub releases\u2026")
        try {
            val req = Request.Builder()
                .url(BuildConfig.UPDATE_ENDPOINT)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "LumenReader/${localVersionName()}")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val s = UpdateState(phase = UpdatePhase.FAILED, message = "Could not reach releases (${resp.code})")
                    _state.value = s
                    return@withContext s
                }
                val json = JSONObject(resp.body?.string().orEmpty())
                val tag = json.optString("tag_name", "").trim()
                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.optJSONObject(i) ?: continue
                        val name = a.optString("name", "")
                        val url = a.optString("browser_download_url", "")
                        if (name.endsWith(".apk", true) && url.isNotBlank()) {
                            apkUrl = url
                            break
                        }
                    }
                }
                val remoteCode = parseVersionCode(tag)
                val localCode = localVersionCode()
                val localName = localVersionName()
                // Compare by versionCode primarily; tag string as fallback
                val isNewer = when {
                    tag.isBlank() -> false
                    remoteCode > 0 && localCode > 0 -> remoteCode > localCode
                    else -> {
                        val clean = tag.removePrefix("v")
                        clean != localName && !localName.startsWith(clean)
                    }
                }
                val s = if (isNewer && !apkUrl.isNullOrBlank()) {
                    UpdateState(
                        phase = UpdatePhase.AVAILABLE,
                        message = "Update available: $tag (you have v$localName)",
                        remoteTag = tag,
                        remoteVersionCode = remoteCode,
                        apkUrl = apkUrl
                    )
                } else if (isNewer && apkUrl.isNullOrBlank()) {
                    UpdateState(
                        phase = UpdatePhase.FAILED,
                        message = "Release $tag has no APK asset",
                        remoteTag = tag
                    )
                } else {
                    UpdateState(
                        phase = UpdatePhase.UP_TO_DATE,
                        message = "You're up to date (v$localName). Latest on GitHub: ${tag.ifBlank { "unknown" }}"
                    )
                }
                _state.value = s
                s
            }
        } catch (e: Exception) {
            val s = UpdateState(phase = UpdatePhase.FAILED, message = e.message ?: "Network error")
            _state.value = s
            s
        }
    }

    suspend fun downloadAndInstall(): UpdateState = withContext(Dispatchers.IO) {
        val cur = _state.value
        val url = cur.apkUrl
        val tag = cur.remoteTag
        if (url.isNullOrBlank()) {
            val s = UpdateState(phase = UpdatePhase.FAILED, message = "No APK URL \u2014 check for updates first")
            _state.value = s
            return@withContext s
        }
        val out = File(updateDir, "lumen-${tag.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.apk")
        if (out.exists() && out.length() > 100_000 && prefs.getString("cached_tag", null) == tag) {
            _state.value = cur.copy(phase = UpdatePhase.READY, message = "Using cached APK", bytesDone = out.length(), bytesTotal = out.length())
            return@withContext installApk(out, tag)
        }
        _state.value = cur.copy(phase = UpdatePhase.DOWNLOADING, message = "Downloading $tag\u2026", bytesDone = 0, bytesTotal = 0)
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "LumenReader/${localVersionName()}")
                .header("Accept", "application/octet-stream")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val s = cur.copy(phase = UpdatePhase.FAILED, message = "Download failed (${resp.code})")
                    _state.value = s
                    return@withContext s
                }
                val total = resp.body?.contentLength() ?: -1L
                val body = resp.body ?: run {
                    val s = cur.copy(phase = UpdatePhase.FAILED, message = "Empty response")
                    _state.value = s
                    return@withContext s
                }
                out.parentFile?.mkdirs()
                val tmp = File(out.absolutePath + ".part")
                tmp.outputStream().use { os ->
                    body.byteStream().use { ins ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            val n = ins.read(buf)
                            if (n <= 0) break
                            os.write(buf, 0, n)
                            done += n
                            _state.value = cur.copy(
                                phase = UpdatePhase.DOWNLOADING,
                                message = "Downloading $tag\u2026",
                                bytesDone = done,
                                bytesTotal = if (total > 0) total else done,
                                apkUrl = url,
                                remoteTag = tag
                            )
                        }
                    }
                }
                if (out.exists()) out.delete()
                tmp.renameTo(out)
                prefs.edit().putString("cached_tag", tag).apply()
                _state.value = cur.copy(
                    phase = UpdatePhase.READY,
                    message = "Download complete",
                    bytesDone = out.length(),
                    bytesTotal = out.length(),
                    apkUrl = url,
                    remoteTag = tag
                )
                return@withContext installApk(out, tag)
            }
        } catch (e: Exception) {
            val s = cur.copy(phase = UpdatePhase.FAILED, message = e.message ?: "Download error")
            _state.value = s
            s
        }
    }

    private fun installApk(apk: File, tag: String): UpdateState {
        return try {
            _state.value = _state.value.copy(phase = UpdatePhase.INSTALLING, message = "Opening installer\u2026")
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apk
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            prefs.edit().putString("installed_tag", tag).apply()
            val s = _state.value.copy(phase = UpdatePhase.READY, message = "Installer opened for $tag")
            _state.value = s
            s
        } catch (e: Exception) {
            val s = _state.value.copy(phase = UpdatePhase.FAILED, message = "Install launch failed: ${e.message}")
            _state.value = s
            s
        }
    }
}
