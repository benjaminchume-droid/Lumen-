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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.lumen.reader.core.AppUpdateManager
import com.lumen.reader.core.SourceStore
import com.lumen.reader.core.UpdatePhase
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onOpenSources: () -> Unit,
    onSignIn: () -> Unit = {},
    onGuest: () -> Unit = {}
) {
    val context = LocalContext.current
    val store = remember { SourceStore(context) }
    val updater = remember { AppUpdateManager(context) }
    val updateState by updater.state.collectAsState()
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var themeMode by remember { mutableStateOf("dark") }
    val installedCount = store.installedIds().size
    val versionName = BuildConfig.VERSION_NAME

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
        SettingsRow(
            icon = Icons.Default.Person,
            title = "Continue as guest",
            subtitle = "Read offline · upgrade anytime in Settings",
            onClick = onGuest
        )
        SettingsRow(
            icon = Icons.Default.Email,
            title = "Sign in to account",
            subtitle = "Email · password or OTP · interests · library sync",
            onClick = onSignIn
        )

        Spacer(modifier = Modifier.height(20.dp))
        SectionLabel("Customization")
        SettingsRow(
            icon = Icons.Default.Settings,
            title = "Theme",
            subtitle = if (themeMode == "dark") "Dark (Frost)" else "Light",
            onClick = { themeMode = if (themeMode == "dark") "light" else "dark" }
        )
        SettingsRow(
            icon = Icons.Default.Source,
            title = "Sources & extensions",
            subtitle = "$installedCount installed · Keiyoushi + LNReader",
            onClick = onOpenSources
        )

        Spacer(modifier = Modifier.height(20.dp))
        SectionLabel("Updates")
        SettingsRow(
            icon = Icons.Default.Refresh,
            title = "Check for updates",
            subtitle = updateState.message.ifBlank { "Fetches latest APK from GitHub releases" },
            onClick = {
                if (updateState.phase != UpdatePhase.CHECKING && updateState.phase != UpdatePhase.DOWNLOADING) {
                    scope.launch {
                        val s = updater.checkForUpdate()
                        if (s.phase == UpdatePhase.AVAILABLE) {
                            updater.downloadAndInstall()
                        }
                    }
                }
            },
            trailing = {
                when (updateState.phase) {
                    UpdatePhase.CHECKING, UpdatePhase.DOWNLOADING, UpdatePhase.INSTALLING ->
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = LumenColors.FrostedBlue
                        )
                    else -> {}
                }
            }
        )
        if (updateState.phase == UpdatePhase.DOWNLOADING || updateState.phase == UpdatePhase.READY) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)) {
                LinearProgressIndicator(
                    progress = { updateState.percent / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = LumenColors.FrostedBlue,
                    trackColor = Color.White.copy(alpha = 0.12f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${updateState.percent}% · ${updateState.bytesDone / 1024} KB" +
                        if (updateState.bytesTotal > 0) " / ${updateState.bytesTotal / 1024} KB" else "",
                    color = LumenColors.MistGray,
                    fontSize = 11.sp
                )
            }
        }
        if (updateState.phase == UpdatePhase.AVAILABLE) {
            Text(
                "Tap again to download & install ${updateState.remoteTag}",
                color = LumenColors.FrostedBlue,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    .clickable {
                        scope.launch { updater.downloadAndInstall() }
                    }
            )
        }

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
