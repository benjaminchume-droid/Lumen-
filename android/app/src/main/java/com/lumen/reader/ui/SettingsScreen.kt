package com.lumen.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.lumen.reader.data.SupabaseClient
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@Composable
fun SettingsScreen(
    onOpenSources: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { SourceStore(context) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var backend by remember { mutableStateOf<SupabaseClient.Health?>(null) }
    var checkingBackend by remember { mutableStateOf(true) }
    var updateMsg by remember { mutableStateOf<String?>(null) }
    var checkingUpdate by remember { mutableStateOf(false) }
    val installedCount = store.installedIds().size

    fun pingBackend() {
        scope.launch {
            checkingBackend = true
            backend = SupabaseClient.checkHealth()
            checkingBackend = false
        }
    }

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
                        updateMsg = "Could not reach releases (${resp.code})"
                        return@use
                    }
                    val json = JSONObject(resp.body?.string().orEmpty())
                    val tag = json.optString("tag_name", "")
                    val current = "v${BuildConfig.VERSION_NAME}"
                    updateMsg = if (tag.isNotBlank() && tag != current && !tag.endsWith(BuildConfig.VERSION_NAME)) {
                        "Update available: $tag (you have $current)"
                    } else {
                        "Up to date ($current)"
                    }
                }
            } catch (e: Exception) {
                updateMsg = e.message ?: "Update check failed"
            }
            checkingUpdate = false
        }
    }

    LaunchedEffect(Unit) { pingBackend() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C0F))
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier = Modifier.height(48.dp))
        Text("Settings", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Lumen v${BuildConfig.VERSION_NAME}",
            color = Color(0xFF64748B),
            fontSize = 12.sp
        )

        Spacer(Modifier = Modifier.height(24.dp))
        SectionLabel("Extensions")
        SettingsRow(
            icon = Icons.Default.Extension,
            title = "Sources",
            subtitle = "$installedCount installed · Keiyoushi + LNReader",
            onClick = onOpenSources
        )

        Spacer(Modifier = Modifier.height(20.dp))
        SectionLabel("Backend")
        SettingsRow(
            icon = Icons.Default.Cloud,
            title = "Supabase Lumen",
            subtitle = when {
                checkingBackend -> "Checking connection…"
                backend?.ok == true -> backend!!.message
                backend != null -> "Error: ${backend!!.message}"
                else -> "Not checked"
            },
            trailing = {
                if (checkingBackend) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF7BC6FF)
                    )
                } else {
                    Text(
                        if (backend?.ok == true) "ONLINE" else "RETRY",
                        color = if (backend?.ok == true) Color(0xFF34D399) else Color(0xFF7BC6FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { pingBackend() }
                    )
                }
            }
        )
        SettingsRow(
            icon = Icons.Default.Storage,
            title = "Project",
            subtitle = "ryoewtikgwmyejrpjgnw · eu-west-1"
        )

        Spacer(Modifier = Modifier.height(20.dp))
        SectionLabel("Updates")
        SettingsRow(
            icon = Icons.Default.Refresh,
            title = "Check for updates",
            subtitle = updateMsg ?: "Idempotent — same release never re-installed",
            onClick = { if (!checkingUpdate) checkUpdate() },
            trailing = {
                if (checkingUpdate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF7BC6FF)
                    )
                }
            }
        )

        Spacer(Modifier = Modifier.height(20.dp))
        SectionLabel("About")
        SettingsRow(
            icon = Icons.Default.Info,
            title = "Lumen Reading OS",
            subtitle = "Extensions · Aggregator · Data-saver downloads"
        )

        Spacer(Modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = Color(0xFF64748B),
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
            .background(Color(0xFF12151C))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF7BC6FF), modifier = Modifier.size(22.dp))
        Spacer(Modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color(0xFF64748B), fontSize = 12.sp)
        }
        trailing?.invoke()
    }
}
