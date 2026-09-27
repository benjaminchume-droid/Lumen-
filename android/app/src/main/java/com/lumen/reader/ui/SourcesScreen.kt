package com.lumen.reader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.reader.core.ExtensionIndexFetcher
import com.lumen.reader.core.ExtensionInstaller
import com.lumen.reader.core.IndexEntry
import com.lumen.reader.core.InstallPhase
import com.lumen.reader.core.MediaKind
import com.lumen.reader.core.SourceStore
import kotlinx.coroutines.launch

@Composable
fun SourcesScreen(onBack: () -> Unit = {}, embedded: Boolean = false) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val store = remember { SourceStore(context) }
    val installer = remember { ExtensionInstaller(context) }
    val scope = rememberCoroutineScope()
    val progressMap by installer.progress.collectAsState()
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var entries by remember { mutableStateOf<List<IndexEntry>>(emptyList()) }
    var installed by remember { mutableStateOf(store.getInstalled()) }
    var filter by remember { mutableStateOf("All") }
    var status by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            status = null
            entries = runCatching { ExtensionIndexFetcher.fetchAll() }
                .onFailure { status = it.message }
                .getOrDefault(emptyList())
            installed = store.getInstalled()
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    val filtered = remember(entries, query, filter, installed) {
        val base = when (filter) {
            "Manga" -> entries.filter { it.kind == MediaKind.MANGA }
            "Novel" -> entries.filter { it.kind == MediaKind.NOVEL }
            "Installed" -> installed
            else -> entries
        }
        if (query.isBlank()) base
        else base.filter {
            it.name.contains(query, true) || it.lang.contains(query, true) || it.id.contains(query, true)
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF0A0C0F)).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(if (embedded) 12.dp else 48.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) { Text("← Back", color = Color(0xFF7BC6FF)) }
            Column(Modifier.weight(1f)) {
                Text("Sources", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Text("Manga · Keiyoushi · Novel · LNReader", color = Color(0xFF64748B), fontSize = 12.sp)
            }
            IconButton(onClick = { reload() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF7BC6FF))
            }
        }
        status?.let {
            Text(it, color = Color(0xFFF87171), fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search sources…", color = Color(0xFF64748B)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF7BC6FF)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF7BC6FF),
                unfocusedBorderColor = Color(0x22FFFFFF),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF7BC6FF),
                focusedContainerColor = Color(0xFF12151C),
                unfocusedContainerColor = Color(0xFF12151C)
            )
        )
        Spacer(Modifier.height(10.dp))
        Row {
            listOf("All", "Manga", "Novel", "Installed").forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF7BC6FF),
                        selectedLabelColor = Color(0xFF0A0C0F),
                        containerColor = Color(0xFF161A22),
                        labelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF7BC6FF))
            }
        } else {
            Text("${filtered.size} extensions", color = Color(0xFF64748B), fontSize = 11.sp)
            LazyColumn(Modifier.fillMaxSize()) {
                items(filtered, key = { it.id }) { entry ->
                    val prog = progressMap[entry.id]
                    val pkgInstalled = installer.isPackageInstalled(entry.pkg)
                    val marked = installed.any { it.id == entry.id } || pkgInstalled
                    val downloading = prog?.phase == InstallPhase.DOWNLOADING || prog?.phase == InstallPhase.INSTALLING
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (entry.kind == MediaKind.MANGA) Color(0xFF1E2A3A) else Color(0xFF2A1E3A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(entry.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                buildString {
                                    append(entry.lang.uppercase())
                                    append(" · ")
                                    append(entry.version)
                                    append(" @")
                                    append(if (entry.repoId == "keiyoushi") "Keiyoushi" else "LNReader")
                                    if (downloading) {
                                        append(" · ")
                                        append(prog?.message ?: "Downloading…")
                                    }
                                },
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (downloading && (prog?.percent ?: 0) > 0) {
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp))
                                        .background(Color(0x22FFFFFF))
                                ) {
                                    Box(
                                        Modifier.fillMaxWidth((prog!!.percent / 100f).coerceIn(0.02f, 1f))
                                            .height(3.dp).background(Color(0xFFA78BFA))
                                    )
                                }
                            }
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                        }
                        if (downloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp).padding(end = 4.dp),
                                color = Color(0xFFA78BFA),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = {
                                scope.launch {
                                    if (!installer.canRequestPackageInstalls()) {
                                        status = "Allow install unknown apps for Lumen"
                                        context.startActivity(installer.openUnknownSourcesSettings())
                                        return@launch
                                    }
                                    val result = installer.downloadAndInstall(entry)
                                    result.onSuccess { apk ->
                                        store.install(entry)
                                        installed = store.getInstalled()
                                        installer.markInstalling(entry.id)
                                        try {
                                            context.startActivity(installer.launchInstall(apk))
                                        } catch (e: Exception) {
                                            status = e.message ?: "Install prompt failed"
                                        }
                                    }.onFailure {
                                        status = it.message
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = if (marked) "Reinstall" else "Download",
                                    tint = if (marked || prog?.phase == InstallPhase.INSTALLED) Color(0xFF34D399) else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
