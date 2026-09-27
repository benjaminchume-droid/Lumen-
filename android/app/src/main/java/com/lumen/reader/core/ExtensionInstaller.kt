package com.lumen.reader.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

enum class InstallPhase { IDLE, DOWNLOADING, READY, INSTALLING, INSTALLED, FAILED }

data class InstallProgress(
    val entryId: String,
    val phase: InstallPhase = InstallPhase.IDLE,
    val bytesDone: Long = 0L,
    val bytesTotal: Long = 0L,
    val message: String = "",
    val localApk: File? = null
) {
    val percent: Int
        get() = when {
            bytesTotal > 0 -> ((bytesDone * 100) / bytesTotal).toInt().coerceIn(0, 100)
            phase == InstallPhase.INSTALLED -> 100
            else -> 0
        }
}

class ExtensionInstaller(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _progress = MutableStateFlow<Map<String, InstallProgress>>(emptyMap())
    val progress: StateFlow<Map<String, InstallProgress>> = _progress.asStateFlow()

    private val extDir: File by lazy {
        File(context.cacheDir, "extensions").also { it.mkdirs() }
    }

    fun isPackageInstalled(pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return false
        return try {
            if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(pkg, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }

    fun openUnknownSourcesSettings(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun apkFileFor(entry: IndexEntry): File {
        val safe = (entry.pkg ?: entry.id).replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(extDir, "$safe-v${entry.version}.apk")
    }

    private fun setProgress(id: String, update: (InstallProgress) -> InstallProgress) {
        val cur = _progress.value[id] ?: InstallProgress(entryId = id)
        _progress.value = _progress.value + (id to update(cur))
    }

    suspend fun downloadAndInstall(entry: IndexEntry): Result<File> = withContext(Dispatchers.IO) {
        val id = entry.id
        val pkg = entry.pkg
        if (pkg != null && isPackageInstalled(pkg)) {
            setProgress(id) {
                it.copy(phase = InstallPhase.INSTALLED, message = "Already installed", bytesDone = 1, bytesTotal = 1)
            }
            return@withContext Result.success(apkFileFor(entry))
        }
        val apkUrl = entry.apkUrl
        if (apkUrl.isNullOrBlank()) {
            setProgress(id) { it.copy(phase = InstallPhase.FAILED, message = "No APK URL in index") }
            return@withContext Result.failure(IllegalStateException("No apkUrl for ${entry.name}"))
        }

        val dest = apkFileFor(entry)
        if (dest.exists() && dest.length() > 10_000L) {
            setProgress(id) {
                it.copy(
                    phase = InstallPhase.READY,
                    bytesDone = dest.length(),
                    bytesTotal = dest.length(),
                    message = "Cached",
                    localApk = dest
                )
            }
            return@withContext Result.success(dest)
        }

        setProgress(id) { it.copy(phase = InstallPhase.DOWNLOADING, message = "Downloading…", bytesDone = 0, bytesTotal = 0) }
        try {
            val req = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "LumenReader/1.5")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    setProgress(id) { it.copy(phase = InstallPhase.FAILED, message = "HTTP ${resp.code}") }
                    return@withContext Result.failure(IllegalStateException("HTTP ${resp.code}"))
                }
                val body = resp.body ?: run {
                    setProgress(id) { it.copy(phase = InstallPhase.FAILED, message = "Empty body") }
                    return@withContext Result.failure(IllegalStateException("Empty body"))
                }
                val total = body.contentLength().coerceAtLeast(0L)
                dest.parentFile?.mkdirs()
                val tmp = File(dest.absolutePath + ".part")
                tmp.outputStream().use { out ->
                    body.byteStream().use { input ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            out.write(buf, 0, n)
                            done += n
                            setProgress(id) {
                                it.copy(
                                    phase = InstallPhase.DOWNLOADING,
                                    bytesDone = done,
                                    bytesTotal = if (total > 0) total else done,
                                    message = "Downloading… ${done / 1024} KB"
                                )
                            }
                        }
                    }
                }
                if (dest.exists()) dest.delete()
                tmp.renameTo(dest)
            }
            setProgress(id) {
                it.copy(
                    phase = InstallPhase.READY,
                    bytesDone = dest.length(),
                    bytesTotal = dest.length(),
                    message = "Ready to install",
                    localApk = dest
                )
            }
            Result.success(dest)
        } catch (e: Exception) {
            setProgress(id) { it.copy(phase = InstallPhase.FAILED, message = e.message ?: "Download failed") }
            Result.failure(e)
        }
    }

    fun launchInstall(apk: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun markInstalling(id: String) {
        setProgress(id) { it.copy(phase = InstallPhase.INSTALLING, message = "Installing…") }
    }

    fun refreshInstalled(id: String, pkg: String?) {
        if (isPackageInstalled(pkg)) {
            setProgress(id) { it.copy(phase = InstallPhase.INSTALLED, message = "Installed", bytesDone = 1, bytesTotal = 1) }
        }
    }
}
