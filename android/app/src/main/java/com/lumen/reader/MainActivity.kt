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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
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
import com.lumen.reader.core.SourceStore
import com.lumen.reader.ui.LumenColors
import com.lumen.reader.ui.LumenTheme
import com.lumen.reader.ui.ReaderScreen
import com.lumen.reader.ui.SAMPLE_MANGA
import com.lumen.reader.ui.SAMPLE_NOVEL
import com.lumen.reader.ui.SampleSeries
import com.lumen.reader.ui.SettingsScreen
import com.lumen.reader.ui.SourcesScreen
import java.util.Calendar

enum class Tab(val label: String, val icon: ImageVector) {
    Library("Library", Icons.Default.List),
    History("History", Icons.Default.Info),
    Home("Home", Icons.Default.Home),
    Updates("Updates", Icons.Default.Notifications),
    More("More", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LumenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LumenColors.SoftBlack
                ) {
                    LumenAppRoot()
                }
            }
        }
    }
}

@Composable
fun LumenAppRoot() {
    var tab by remember { mutableStateOf(Tab.Home) }
    var sourcesOpen by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf<SampleSeries?>(null) }

    BackHandler(enabled = sourcesOpen || reading != null) {
        when {
            reading != null -> reading = null
            sourcesOpen -> sourcesOpen = false
        }
    }

    if (reading != null) {
        ReaderScreen(series = reading!!, onClose = { reading = null })
        return
    }

    if (sourcesOpen) {
        SourcesScreen(onBack = { sourcesOpen = false })
        return
    }

    Scaffold(
        containerColor = LumenColors.SoftBlack,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F1218),
                contentColor = LumenColors.FrostedBlue
            ) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LumenColors.FrostedBlue,
                            selectedTextColor = LumenColors.FrostedBlue,
                            unselectedIconColor = LumenColors.MistGray,
                            unselectedTextColor = LumenColors.MistGray,
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
                Tab.Library -> LibraryTab(
                    onOpenSources = { sourcesOpen = true },
                    onRead = { reading = it }
                )
                Tab.History -> HistoryTab(onRead = { reading = it })
                Tab.Home -> HomeTab(
                    onOpenSources = { sourcesOpen = true },
                    onRead = { reading = it }
                )
                Tab.Updates -> UpdatesTab(onRead = { reading = it })
                Tab.More -> SettingsScreen(onOpenSources = { sourcesOpen = true })
            }
        }
    }
}

@Composable
private fun HomeTab(onOpenSources: () -> Unit, onRead: (SampleSeries) -> Unit) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LumenColors.SoftBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            "Reading",
            color = LumenColors.FrostedBlue.copy(alpha = 0.65f),
            fontSize = 11.sp,
            letterSpacing = 1.5.sp
        )
        Text(
            greeting,
            color = LumenColors.FrostWhite,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LumenColors.DeepGraphite)
                    .clickable(onClick = onOpenSources)
                    .padding(14.dp)
            ) {
                Text("Explore sources", color = LumenColors.MistGray, fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(LumenColors.FrostedBlue)
                    .clickable { onRead(SAMPLE_NOVEL) }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text("Open novel", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        Text(
            "Continue reading",
            color = LumenColors.MistGray.copy(alpha = 0.75f),
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        SeriesCard(
            title = SAMPLE_NOVEL.title,
            subtitle = SAMPLE_NOVEL.chapterTitle,
            badge = "Novel",
            onClick = { onRead(SAMPLE_NOVEL) }
        )
        Spacer(modifier = Modifier.height(10.dp))
        SeriesCard(
            title = SAMPLE_MANGA.title,
            subtitle = SAMPLE_MANGA.chapterTitle,
            badge = "Manga",
            onClick = { onRead(SAMPLE_MANGA) }
        )

        Spacer(modifier = Modifier.height(28.dp))
        Text(
            "Sources",
            color = LumenColors.MistGray.copy(alpha = 0.75f),
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(LumenColors.DeepGraphite)
                .clickable(onClick = onOpenSources)
                .padding(18.dp)
        ) {
            Column {
                Text("Install extensions", color = LumenColors.FrostWhite, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Manga and novel sources from community indexes.",
                    color = LumenColors.MistGray.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun LibraryTab(onOpenSources: () -> Unit, onRead: (SampleSeries) -> Unit) {
    val context = LocalContext.current
    val store = remember { SourceStore(context) }
    val installed = remember { store.getInstalled() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LumenColors.SoftBlack)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text("Library", color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Your series and installed sources",
            color = LumenColors.MistGray.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(20.dp))

        SeriesCard(SAMPLE_NOVEL.title, SAMPLE_NOVEL.chapterTitle, "Novel") { onRead(SAMPLE_NOVEL) }
        Spacer(modifier = Modifier.height(10.dp))
        SeriesCard(SAMPLE_MANGA.title, SAMPLE_MANGA.chapterTitle, "Manga") { onRead(SAMPLE_MANGA) }

        Spacer(modifier = Modifier.height(20.dp))
        if (installed.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(LumenColors.DeepGraphite)
                    .padding(20.dp)
            ) {
                Column {
                    Text("No sources yet", color = LumenColors.FrostWhite, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Add manga or novel sources to expand your library.",
                        color = LumenColors.MistGray.copy(alpha = 0.75f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Manage sources",
                        color = LumenColors.SoftBlack,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(LumenColors.FrostedBlue)
                            .clickable(onClick = onOpenSources)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        } else {
            Text(
                "${installed.size} sources",
                color = LumenColors.MistGray.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(installed, key = { it.id }) { src ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(LumenColors.DeepGraphite)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(src.name, color = LumenColors.FrostWhite, fontSize = 14.sp)
                            Text(
                                "${src.lang} · v${src.version}",
                                color = LumenColors.MistGray.copy(alpha = 0.7f),
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
private fun HistoryTab(onRead: (SampleSeries) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LumenColors.SoftBlack)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text("Reading history", color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Pick up where you left off",
            color = LumenColors.MistGray.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        SeriesCard(SAMPLE_NOVEL.title, "Recently opened", "Novel") { onRead(SAMPLE_NOVEL) }
        Spacer(modifier = Modifier.height(10.dp))
        SeriesCard(SAMPLE_MANGA.title, "Recently opened", "Manga") { onRead(SAMPLE_MANGA) }
    }
}

@Composable
private fun UpdatesTab(onRead: (SampleSeries) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LumenColors.SoftBlack)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            "Recent updates",
            color = LumenColors.FrostWhite,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "New chapters from your library",
            color = LumenColors.MistGray.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(LumenColors.DeepGraphite)
                .padding(20.dp)
        ) {
            Column {
                Text("No new updates", color = LumenColors.FrostWhite, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "When sources have new chapters, they will appear here.",
                    color = LumenColors.MistGray.copy(alpha = 0.75f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "Read a sample",
                    color = LumenColors.FrostedBlue,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onRead(SAMPLE_NOVEL) }
                )
            }
        }
    }
}

@Composable
private fun SeriesCard(
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LumenColors.DeepGraphite)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x227BC6FF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                badge.take(1),
                color = LumenColors.FrostedBlue,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = LumenColors.FrostWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = LumenColors.MistGray.copy(alpha = 0.75f), fontSize = 12.sp)
        }
        Text(
            badge,
            color = LumenColors.FrostedBlue.copy(alpha = 0.9f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
