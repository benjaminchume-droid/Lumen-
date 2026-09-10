package com.lumen.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.lumen.reader.core.MediaKind
import com.lumen.reader.core.SourceStore
import com.lumen.reader.ui.LumenTheme
import com.lumen.reader.ui.SettingsScreen
import com.lumen.reader.ui.SourcesScreen

enum class Tab(val label: String, val icon: ImageVector) {
    Library("Library", Icons.Default.Home),
    Browse("Browse", Icons.Default.Search),
    Sources("Sources", Icons.Default.List),
    Settings("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LumenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0C0F)
                ) {
                    LumenAppRoot()
                }
            }
        }
    }
}

@Composable
fun LumenAppRoot() {
    var tab by remember { mutableStateOf(Tab.Library) }
    var sourcesOverlay by remember { mutableStateOf(false) }

    BackHandler(enabled = sourcesOverlay) {
        sourcesOverlay = false
    }

    if (sourcesOverlay) {
        SourcesScreen(onBack = { sourcesOverlay = false })
        return
    }

    Scaffold(
        containerColor = Color(0xFF0A0C0F),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F1218),
                contentColor = Color(0xFF7BC6FF)
            ) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF7BC6FF),
                            selectedTextColor = Color(0xFF7BC6FF),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B),
                            indicatorColor = Color(0x227BC6FF)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (tab) {
                Tab.Library -> LibraryScreen(onOpenSources = { tab = Tab.Sources })
                Tab.Browse -> BrowseScreen(onOpenSources = { tab = Tab.Sources })
                Tab.Sources -> SourcesScreen(embedded = true)
                Tab.Settings -> SettingsScreen(onOpenSources = { sourcesOverlay = true })
            }
        }
    }
}

@Composable
fun LibraryScreen(onOpenSources: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SourceStore(context) }
    val installed = remember { store.getInstalled() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C0F))
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text("Library", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Your series appear here after browsing sources",
            color = Color(0xFF64748B),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (installed.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF12151C))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No sources installed yet", color = Color.White, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Install manga (Keiyoushi) or novel (LNReader) sources to start reading.",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Go to Sources →",
                        color = Color(0xFF0A0C0F),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF7BC6FF))
                            .clickable(onClick = onOpenSources)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        } else {
            Text(
                "${installed.size} sources ready",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(installed, key = { it.id }) { src ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF12151C))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (src.kind == MediaKind.MANGA) "Manga" else "Novel",
                            color = if (src.kind == MediaKind.MANGA) Color(0xFF7BC6FF) else Color(0xFFA78BFA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1AFFFFFF))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.padding(start = 12.dp))
                        Column {
                            Text(src.name, color = Color.White, fontSize = 14.sp)
                            Text(
                                "${src.lang} · v${src.version}",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BrowseScreen(onOpenSources: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C0F))
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text("Browse", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Discover from installed sources",
            color = Color(0xFF64748B),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF12151C))
                .padding(24.dp)
        ) {
            Column {
                Text("Install a source to browse", color = Color.White, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Manga extensions come from Keiyoushi (Mihon). Novel plugins come from LNReader. Only indexes are downloaded until you install.",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Manage Sources →",
                    color = Color(0xFF0A0C0F),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF7BC6FF))
                        .clickable(onClick = onOpenSources)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}
