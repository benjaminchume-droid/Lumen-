package com.lumen.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.reader.BuildConfig
import com.lumen.reader.core.SourceStore
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@Composable
fun SettingsScreen(onOpenSources: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SourceStore(context) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var themeMode by remember { mutableStateOf("dark") }
    var updateMsg by remember { mutableStateOf<String?>(null) }
    var checkingUpdate by remember { mutableStateOf(false) }
    val installedCount = store.installedIds().size
    val versionName = BuildConfig.VERSION_NAME

    fun checkUpdate() {
        scope.launch {
            checkingUpdate = true
            updateMsg = null
            try {
                val client = OkHttpClient()
                val req = Request.Builder()
                    .url(BuildConfig.UPDATE_ENDPOINT)
                    .header("Accept", "application/vnd.github+json")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        updateMsg = "Could not check right now"
                        return@use
                    }
                    val json = JSONObject(resp.body?.string().orEmpty())
                    val tag = json.optString("tag_name", "")
                    updateMsg = if (tag.isNotBlank() && !tag.endsWith(versionName)) {
                        "Update available: $tag"
                    } else {
                        "You're up to date"
                    }
                }
            } catch (_: Exception) {
                updateMsg = "Could not check right now"
            }
            checkingUpdate = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LumenColors.SoftBlack)
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text("Settings", color = LumenColors.FrostedBlue.copy(alpha = 0.7f), fontSize = 11.sp, letterSpacing = 1.5.sp)
        Text("System settings", color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text("Lumen v$versionName", color = LumenColors.MistGray.copy(alpha = 0.65f), fontSize = 12.sp)

        Spacer(modifier = Modifier.height(24.dp))
        SectionLabel("Account")
        SettingsRow(icon = Icons.Default.Person, title = "Continue as guest", subtitle = "Read offline · upgrade anytime in Settings")
        SettingsRow(icon = Icons.Default.Email, title = "Sign in / Sign up", subtitle = "Email OTP · 6-digit code · library sync")

        Spacer(modifier = Modifier.height(20.dp))
        SectionLabel("Customization")
        SettingsRow(
            icon = Icons.Default.Settings,
            title = "Theme",
            subtitle = themeMode.replaceFirstChar { it.uppercase() },
            trailing = {
                Row {
                    listOf("light", "dark", "amoled").forEach { mode ->
                        val selected = themeMode == mode
                        Text(
                            mode.uppercase().take(5),
                            color = if (selected) LumenColors.SoftBlack else LumenColors.MistGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) LumenColors.FrostedBlue else Color.Transparent)
                                .clickable { themeMode = mode }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        )
        SettingsRow(icon = Icons.Default.Settings, title = "Reader defaults", subtitle = "Font, size, and page layout")

        Spacer(modifier = Modifier.height(20.dp))
        SectionLabel("Sources")
        SettingsRow(
            icon = Icons.Default.List,
            title = "Extensions",
            subtitle = if (installedCount == 0) "None installed yet" else "$installedCount installed",
            onClick = onOpenSources
        )

        Spacer(modifier = Modifier.height(20.dp))
        SectionLabel("Updates")
        SettingsRow(
            icon = Icons.Default.Refresh,
            title = "Check for updates",
            subtitle = updateMsg ?: "Only installs a newer release once",
            onClick = { if (!checkingUpdate) checkUpdate() },
            trailing = {
                if (checkingUpdate) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LumenColors.FrostedBlue)
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))
        SectionLabel("Legal")
        SettingsRow(icon = Icons.Default.Info, title = "Privacy policy", subtitle = "Reading data stays on device unless you sign in")
        SettingsRow(icon = Icons.Default.Info, title = "Terms", subtitle = "Use extensions only where permitted")
        SettingsRow(icon = Icons.Default.Info, title = "Open source notices", subtitle = "Third-party indexes and libraries")

        Spacer(modifier = Modifier.height(20.dp))
        SectionLabel("About")
        SettingsRow(icon = Icons.Default.Info, title = "Lumen", subtitle = "Read novels and manga from your sources")

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = LumenColors.MistGray.copy(alpha = 0.55f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(LumenColors.DeepGraphite)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = LumenColors.FrostedBlue, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = LumenColors.FrostWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = LumenColors.MistGray, fontSize = 12.sp)
        }
        trailing?.invoke()
    }
}
