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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.reader.core.MediaKind
import com.lumen.reader.core.SourceStore
import com.lumen.reader.ui.CatalogSeries
import com.lumen.reader.ui.LumenColors
import com.lumen.reader.ui.LumenTheme
import com.lumen.reader.ui.ReaderScreen
import com.lumen.reader.ui.SAMPLE_MANGA
import com.lumen.reader.ui.SAMPLE_NOVEL
import com.lumen.reader.ui.SampleSeries
import com.lumen.reader.ui.SeriesChapter
import com.lumen.reader.ui.SeriesDetailScreen
import com.lumen.reader.ui.SettingsScreen
import com.lumen.reader.ui.SourcesScreen
import com.lumen.reader.ui.sampleCatalogFromInstalled

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
                Surface(modifier = Modifier.fillMaxSize(), color = LumenColors.SoftBlack) {
                    LumenAppRoot()
                }
            }
        }
    }
}

@Composable
fun LumenAppRoot() {
    val context = LocalContext.current
    val store = remember { SourceStore(context) }

    var tab by remember { mutableStateOf(Tab.Home) }
    var sourcesOpen by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<CatalogSeries?>(null) }
    var reading by remember { mutableStateOf<SampleSeries?>(null) }
    var libraryIds by remember { mutableStateOf(setOf<String>()) }
    var installTick by remember { mutableStateOf(0) }

    val feed: List<CatalogSeries> = remember(installTick) {
        val installed = store.getInstalled()
        if (installed.isEmpty()) emptyList()
        else installed.flatMap { src ->
            val kind = if (src.kind == MediaKind.MANGA) "manga" else "novel"
            sampleCatalogFromInstalled(src.name, kind)
        }
    }

    BackHandler(enabled = sourcesOpen || detail != null || reading != null) {
        when {
            reading != null -> reading = null
            detail != null -> detail = null
            sourcesOpen -> { sourcesOpen = false; installTick++ }
        }
    }

    if (reading != null) {
        ReaderScreen(series = reading!!, onClose = { reading = null })
        return
    }

    if (detail != null) {
        SeriesDetailScreen(
            series = detail!!,
            onBack = { detail = null },
            inLibrary = libraryIds.contains(detail!!.id),
            onToggleLibrary = {
                libraryIds =
                    if (libraryIds.contains(detail!!.id)) libraryIds - detail!!.id
                    else libraryIds + detail!!.id
            },
            onStartChapter = { ch -> reading = chapterToReader(detail!!, ch) }
        )
        return
    }

    if (sourcesOpen) {
        SourcesScreen(onBack = { sourcesOpen = false; installTick++ })
        return
    }

    Scaffold(
        containerColor = LumenColors.SoftBlack,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0F1218), contentColor = LumenColors.FrostedBlue) {
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.Home -> HomeGrid(feed, { sourcesOpen = true }, { detail = it })
                Tab.Library -> LibraryGrid(
                    feed.filter { libraryIds.contains(it.id) }.ifEmpty { feed.take(2) },
                    { sourcesOpen = true },
                    { detail = it }
                )
                Tab.History -> HistorySimple { detail = feed.firstOrNull() }
                Tab.Updates -> UpdatesSimple { sourcesOpen = true }
                Tab.More -> SettingsScreen(onOpenSources = { sourcesOpen = true })
            }
        }
    }
}

private fun chapterToReader(series: CatalogSeries, ch: SeriesChapter): SampleSeries {
    val isNovel = series.kind.equals("novel", ignoreCase = true)
    return if (isNovel) {
        SAMPLE_NOVEL.copy(
            id = series.id,
            title = series.title,
            author = series.author,
            chapterTitle = ch.title,
            chapterId = ch.id,
            pages = listOf(
                series.description,
                "You opened ${ch.title}. Scroll to continue.",
                "Comments and likes on this chapter use the external series id: ${series.id}",
                "Chapter key: ${ch.id}"
            )
        )
    } else {
        SAMPLE_MANGA.copy(
            id = series.id,
            title = series.title,
            author = series.author,
            chapterTitle = ch.title,
            chapterId = ch.id,
            pages = listOf("Page 1", "Page 2", "Page 3", "Page 4", "Page 5")
        )
    }
}

@Composable
private fun HomeGrid(
    feed: List<CatalogSeries>,
    onOpenSources: () -> Unit,
    onOpenSeries: (CatalogSeries) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(LumenColors.SoftBlack).padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text("Home", color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text(
            if (feed.isEmpty()) "Install extensions to fill this feed" else "From your installed sources",
            color = LumenColors.MistGray.copy(alpha = 0.75f),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (feed.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(LumenColors.DeepGraphite)
                    .padding(20.dp)
            ) {
                Column {
                    Text("No extensions yet", color = LumenColors.FrostWhite, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Open More → Sources, install Keiyoushi or LNReader. Titles from those sources appear here.",
                        color = LumenColors.MistGray.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Install extensions",
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
            Text("Weekly featured", color = LumenColors.MistGray.copy(alpha = 0.7f), fontSize = 11.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(feed, key = { it.id }) { series ->
                    CoverCard(series = series, onClick = { onOpenSeries(series) })
                }
            }
        }
    }
}

@Composable
private fun LibraryGrid(
    feed: List<CatalogSeries>,
    onOpenSources: () -> Unit,
    onOpenSeries: (CatalogSeries) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(LumenColors.SoftBlack).padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text("Library", color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        if (feed.isEmpty()) {
            Text("Install sources to add titles", color = LumenColors.FrostedBlue, modifier = Modifier.clickable(onClick = onOpenSources))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(feed, key = { it.id }) { series ->
                    CoverCard(series = series, onClick = { onOpenSeries(series) })
                }
            }
        }
    }
}

@Composable
private fun CoverCard(series: CatalogSeries, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(series.coverHint), LumenColors.DeepGraphite))),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                series.kind.uppercase(),
                color = LumenColors.FrostedBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(10.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(series.title, color = LumenColors.FrostWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(series.genres.firstOrNull() ?: series.sourceName, color = LumenColors.MistGray.copy(alpha = 0.75f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HistorySimple(onOpenSeries: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(LumenColors.SoftBlack).padding(20.dp)) {
        Spacer(modifier = Modifier.height(36.dp))
        Text("History", color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Resume last title", color = LumenColors.FrostedBlue, modifier = Modifier.clickable(onClick = onOpenSeries))
    }
}

@Composable
private fun UpdatesSimple(onOpenSources: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(LumenColors.SoftBlack).padding(20.dp)) {
        Spacer(modifier = Modifier.height(36.dp))
        Text("Updates", color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("New chapters from installed sources will appear here.", color = LumenColors.MistGray, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Manage extensions", color = LumenColors.FrostedBlue, modifier = Modifier.clickable(onClick = onOpenSources))
    }
}
