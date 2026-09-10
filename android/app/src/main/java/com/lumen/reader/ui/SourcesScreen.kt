package com.lumen.reader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.reader.core.ExtensionIndexFetcher
import com.lumen.reader.core.IndexEntry
import com.lumen.reader.core.MediaKind
import com.lumen.reader.core.SourceStore
import kotlinx.coroutines.launch

enum class SourceFilter { ALL, MANGA, NOVEL, INSTALLED }

@Composable
fun SourcesScreen(
    onBack: (() -> Unit)? = null,
    embedded: Boolean = false
) {
    val context = LocalContext.current
    val store = remember { SourceStore(context) }
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var catalog by remember { mutableStateOf<List<IndexEntry>>(emptyList()) }
    var installed by remember { mutableStateOf(store.getInstalled()) }
    var filter by remember { mutableStateOf(SourceFilter.ALL) }
    var query by remember { mutableStateOf("") }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            runCatching { ExtensionIndexFetcher.fetchAll() }
                .onSuccess { catalog = it }
                .onFailure { error = it.message ?: "Failed to load indexes" }
            installed = store.getInstalled()
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    if (onBack != null) {
        BackHandler { onBack() }
    }

    val installedIds = remember(installed) { installed.map { it.id }.toSet() }

    val filtered = remember(catalog, installed, filter, query) {
        val base = when (filter) {
            SourceFilter.ALL -> catalog
            SourceFilter.MANGA -> catalog.filter { it.kind == MediaKind.MANGA }
            SourceFilter.NOVEL -> catalog.filter { it.kind == MediaKind.NOVEL }
            SourceFilter.INSTALLED -> installed
        }
        if (query.isBlank()) base
        else base.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.lang.contains(query, ignoreCase = true) ||
                it.id.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C0F))
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(if (embedded) 12.dp else 48.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF7BC6FF)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Sources",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Manga · Keiyoushi  ·  Novel · LNReader",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = { reload() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF7BC6FF))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search sources…", color = Color(0xFF64748B)) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF7BC6FF))
            },
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

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                SourceFilter.ALL to "All",
                SourceFilter.MANGA to "Manga",
                SourceFilter.NOVEL to "Novel",
                SourceFilter.INSTALLED to "Installed"
            ).forEach { (f, label) ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF7BC6FF),
                        selectedLabelColor = Color(0xFF0A0C0F),
                        containerColor = Color(0xFF161A22),
                        labelColor = Color(0xFF94A3B8)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF7BC6FF))
                        Spacer(Modifier = Modifier.height(12.dp))
                        Text("Loading extension indexes…", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                }
            }
            error != null && catalog.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error ?: "Error", color = Color(0xFFF87171), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Tap refresh to retry",
                            color = Color(0xFF7BC6FF),
                            modifier = Modifier.clickable { reload() }
                        )
                    }
                }
            }
            else -> {
                Text(
                    "${filtered.size} sources",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { entry ->
                        SourceRow(
                            entry = entry,
                            installed = installedIds.contains(entry.id),
                            onToggle = {
                                if (store.isInstalled(entry.id)) store.uninstall(entry.id)
                                else store.install(entry)
                                installed = store.getInstalled()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceRow(
    entry: IndexEntry,
    installed: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF12151C))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (entry.kind == MediaKind.MANGA) Color(0x227BC6FF) else Color(0x22A78BFA)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (entry.kind == MediaKind.MANGA) "M" else "N",
                color = if (entry.kind == MediaKind.MANGA) Color(0xFF7BC6FF) else Color(0xFFA78BFA),
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                buildString {
                    append(entry.lang.uppercase())
                    append(" · v")
                    append(entry.version)
                    append(" · ")
                    append(if (entry.kind == MediaKind.MANGA) "Manga" else "Novel")
                    if (entry.nsfw) append(" · NSFW")
                },
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onToggle) {
            Icon(
                if (installed) Icons.Default.Check else Icons.Default.Add,
                contentDescription = if (installed) "Installed" else "Install",
                tint = if (installed) Color(0xFF34D399) else Color(0xFF7BC6FF)
            )
        }
    }
}
