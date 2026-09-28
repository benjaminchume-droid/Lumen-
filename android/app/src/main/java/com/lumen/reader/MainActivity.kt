package com.lumen.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lumen.reader.core.Aggregator
import com.lumen.reader.core.CatalogService
import com.lumen.reader.core.ChapterMeta
import com.lumen.reader.core.SourcePriority
import com.lumen.reader.core.ExtensionRuntime
import com.lumen.reader.core.ExtensionInstaller
import com.lumen.reader.core.IndexEntry
import com.lumen.reader.core.SecureDownloadStore
import com.lumen.reader.core.SourceStore
import com.lumen.reader.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                Surface(Modifier.fillMaxSize(), color = LumenColors.SoftBlack) { LumenAppRoot() }
            }
        }
    }
}

@Composable
fun LumenAppRoot() {
    val context = LocalContext.current
    val store = remember { SourceStore(context) }
    val installer = remember { ExtensionInstaller(context) }
    val dlStore = remember { SecureDownloadStore(context) }
    val runtime = remember { ExtensionRuntime(context) }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(Tab.Home) }
    var sourcesOpen by remember { mutableStateOf(false) }
    var authOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var creatorOpen by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<CatalogSeries?>(null) }
    var reading by remember { mutableStateOf<SampleSeries?>(null) }
    var libraryIds by remember { mutableStateOf(setOf<String>()) }
    var history by remember { mutableStateOf<List<CatalogSeries>>(emptyList()) }
    var installTick by remember { mutableStateOf(0) }
    var installed by remember { mutableStateOf(store.getInstalled()) }
    var feed by remember { mutableStateOf<List<CatalogSeries>>(emptyList()) }
    var feedLoading by remember { mutableStateOf(false) }
    var feedError by remember { mutableStateOf<String?>(null) }

    fun refreshInstalled() {
        installed = store.getInstalled().filter { e ->
            if (e.pkg.isNullOrBlank()) true
            else installer.isPackageInstalled(e.pkg) || store.isInstalled(e.id)
        }
    }

    fun loadFeed() {
        scope.launch {
            feedLoading = true
            feedError = null
            refreshInstalled()
            val withSite = installed.filter {
                (!it.site.isNullOrBlank() && it.site!!.startsWith("http")) || !it.pkg.isNullOrBlank()
            }
            if (withSite.isEmpty() && installed.isEmpty()) {
                feed = emptyList()
                feedLoading = false
                return@launch
            }
            val live = withContext(Dispatchers.IO) {
                runtime.fetchAllPopular(withSite.ifEmpty { installed }, perSource = 80, maxConcurrent = 8)
            }
            feed = live.map { ls ->
                CatalogSeries(
                    id = ls.id, title = ls.title,
                    author = ls.author.ifBlank { ls.sourceName },
                    sourceName = ls.sourceName, kind = ls.kind,
                    description = ls.description,
                    genres = ls.genres.ifEmpty { listOf(ls.sourceName) },
                    status = "Ongoing", chapters = emptyList(),
                    coverHint = 0xFF1E2A3A, coverUrl = ls.coverUrl,
                    seriesUrl = ls.url, sourceId = ls.sourceId
                )
            }
            feedLoading = false
            if (feed.isEmpty() && installed.isNotEmpty()) {
                feedError = "No listings from installed sources yet."
            }
        }
    }

    LaunchedEffect(installTick) { loadFeed() }

    BackHandler(enabled = authOpen || sourcesOpen || settingsOpen || creatorOpen || detail != null || reading != null) {
        when {
            reading != null -> reading = null
            detail != null -> detail = null
            creatorOpen -> creatorOpen = false
            settingsOpen -> settingsOpen = false
            authOpen -> authOpen = false
            sourcesOpen -> { sourcesOpen = false; installTick++ }
        }
    }

    if (reading != null) {
        ReaderScreen(series = reading!!, onClose = { reading = null })
        return
    }
    if (detail != null) {
        SeriesDetailScreen(
            series = detail!!, onBack = { detail = null },
            inLibrary = libraryIds.contains(detail!!.id),
            onToggleLibrary = {
                libraryIds = if (libraryIds.contains(detail!!.id)) libraryIds - detail!!.id
                else libraryIds + detail!!.id
            },
            onStartChapter = { ch ->
                val series = detail!!
                history = listOf(series) + history.filter { it.id != series.id }
                scope.launch {
                    val isNovel = series.kind.equals("novel", ignoreCase = true)
                    val cached = dlStore.loadChapter(ch.id)
                    val pages = cached ?: withContext(Dispatchers.IO) {
                        CatalogService.fetchChapterPages(ch.id, isNovel)
                    }
                    reading = if (isNovel) {
                        SAMPLE_NOVEL.copy(
                            id = series.id, title = series.title, author = series.author,
                            chapterTitle = ch.title, chapterId = ch.id,
                            pages = pages.ifEmpty { listOf("No text extracted.") }
                        )
                    } else {
                        SAMPLE_MANGA.copy(
                            id = series.id, title = series.title, author = series.author,
                            chapterTitle = ch.title, chapterId = ch.id,
                            pages = pages.ifEmpty { listOf("No pages found.") }
                        )
                    }
                }
            },
            onDownloadChapters = { chapters ->
                scope.launch {
                    val series = detail ?: return@launch
                    val isNovel = series.kind.equals("novel", ignoreCase = true)
                    for (ch in chapters) {
                        if (dlStore.isDownloaded(ch.id)) continue
                        val pages = withContext(Dispatchers.IO) {
                            CatalogService.fetchChapterPages(ch.id, isNovel)
                        }
                        dlStore.saveChapter(ch.id, series.id, ch.title, pages)
                    }
                }
            }
        )
        return
    }
    if (authOpen) {
        AuthScreen(onBack = { authOpen = false }, onComplete = { authOpen = false })
        return
    }
    if (creatorOpen) {
        CreatorStudioScreen(onBack = { creatorOpen = false }, onNeedAuth = { creatorOpen = false; authOpen = true })
        return
    }
    if (settingsOpen) {
        SettingsScreen(
            onOpenSources = { settingsOpen = false; sourcesOpen = true },
            onSignIn = { settingsOpen = false; authOpen = true },
            onGuest = { settingsOpen = false }
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
                        selected = tab == t, onClick = { tab = t },
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
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.Home -> HomeGrid(feed, feedLoading, feedError, installed.size,
                    { sourcesOpen = true },
                    { s -> openDetail(s, installed, runtime, scope) { detail = it } },
                    { loadFeed() })
                Tab.Library -> {
                    val lib = feed.filter { libraryIds.contains(it.id) }
                    Column(Modifier.fillMaxSize().background(LumenColors.SoftBlack).padding(16.dp)) {
                        Spacer(Modifier.height(48.dp))
                        Text("Library", color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                        if (lib.isEmpty()) Text("Add titles from Home.", color = LumenColors.MistGray, fontSize = 13.sp)
                        else lib.forEach { s ->
                            Text(s.title, color = LumenColors.FrostedBlue, modifier = Modifier
                                .clickable { openDetail(s, installed, runtime, scope) { detail = it } }
                                .padding(vertical = 8.dp))
                        }
                    }
                }
                Tab.History -> {
                    Column(Modifier.fillMaxSize().background(LumenColors.SoftBlack).padding(20.dp)) {
                        Spacer(Modifier.height(36.dp))
                        Text("History", color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                        if (history.isEmpty()) Text("Nothing read yet.", color = LumenColors.MistGray, fontSize = 13.sp)
                        else history.forEach { s ->
                            Text(s.title, color = LumenColors.FrostedBlue, modifier = Modifier
                                .clickable { detail = s }.padding(vertical = 10.dp))
                        }
                    }
                }
                Tab.Updates -> {
                    Column(Modifier.fillMaxSize().background(LumenColors.SoftBlack).padding(20.dp)) {
                        Spacer(Modifier.height(36.dp))
                        Text("Updates", color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                        Text("Manage extensions", color = LumenColors.FrostedBlue,
                            modifier = Modifier.clickable { sourcesOpen = true }.padding(top = 12.dp))
                    }
                }
                Tab.More -> MoreScreen(
                    onSignIn = { authOpen = true },
                    onOpenSources = { sourcesOpen = true },
                    onOpenDownloads = { },
                    onOpenSettings = { settingsOpen = true },
                    onOpenCreator = {
                        if (com.lumen.reader.data.AuthSession.isSignedIn(context)) creatorOpen = true
                        else authOpen = true
                    },
                    onOpenProfile = {
                        if (!com.lumen.reader.data.AuthSession.isSignedIn(context)) authOpen = true
                    }
                )
            }
        }
    }
}

private fun openDetail(
    series: CatalogSeries, installed: List<IndexEntry>,
    runtime: ExtensionRuntime,
    scope: kotlinx.coroutines.CoroutineScope, setDetail: (CatalogSeries) -> Unit
) {
    val entry = installed.find { it.id == series.sourceId }
        ?: installed.find { series.id.startsWith(it.id) }
    if (entry == null || series.seriesUrl.isNullOrBlank()) {
        setDetail(series); return
    }
    scope.launch {
        val (live, chapters) = withContext(Dispatchers.IO) {
            val reflected = runtime.tryReflectChapters(entry, series.seriesUrl!!)
            val scraped = CatalogService.fetchDetailsAndChapters(series.seriesUrl!!, entry)
            val chapterLists = buildList {
                if (reflected.isNotEmpty()) {
                    add(reflected.map {
                        ChapterMeta(url = it.url, name = it.title, number = it.number, sourceId = entry.id)
                    })
                }
                if (scraped.second.isNotEmpty()) {
                    add(scraped.second.map {
                        ChapterMeta(url = it.url, name = it.title, number = it.number, sourceId = entry.id + "_web")
                    })
                }
            }
            val merged = if (chapterLists.size > 1) {
                Aggregator.mergeChapters(
                    chapterLists,
                    listOf(
                        SourcePriority(entry.id, priority = 10, qualityScore = 90),
                        SourcePriority(entry.id + "_web", priority = 5, qualityScore = 70)
                    )
                ).map { CatalogService.LiveChapter(it.primary.url, it.name, it.primary.url, it.number) }
            } else {
                reflected.ifEmpty { scraped.second }
            }
            scraped.first to merged
        }
        setDetail(series.copy(
            title = live.title.ifBlank { series.title },
            author = live.author.ifBlank { series.author },
            description = live.description.ifBlank { series.description },
            genres = live.genres.ifEmpty { series.genres },
            coverUrl = live.coverUrl ?: series.coverUrl,
            chapters = chapters.mapIndexed { i, c ->
                SeriesChapter(id = c.url, title = c.title, number = c.number.toInt().coerceAtLeast(i + 1))
            }
        ))
    }
}

@Composable
private fun HomeGrid(
    feed: List<CatalogSeries>, loading: Boolean, error: String?, installedCount: Int,
    onOpenSources: () -> Unit, onOpenSeries: (CatalogSeries) -> Unit, onRefresh: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(LumenColors.SoftBlack).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(48.dp))
        Text("Home", color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text(
            when {
                loading -> "Loading series from installed sources\u2026"
                installedCount == 0 -> "Install extensions to fill this feed"
                feed.isEmpty() -> "$installedCount source(s) · no listings yet"
                else -> "${feed.size} titles from $installedCount source(s)"
            },
            color = LumenColors.MistGray.copy(alpha = 0.75f), fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LumenColors.FrostedBlue)
            }
            installedCount == 0 -> Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(LumenColors.DeepGraphite).padding(20.dp)
            ) {
                Column {
                    Text("No extensions yet", color = LumenColors.FrostWhite, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Open Sources — full Keiyoushi + LNReader indexes. Enable any source; Home loads from all installed.",
                        color = LumenColors.MistGray.copy(alpha = 0.8f), fontSize = 13.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("Install extensions", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(LumenColors.FrostedBlue)
                            .clickable(onClick = onOpenSources).padding(horizontal = 14.dp, vertical = 10.dp))
                }
            }
            feed.isEmpty() -> Column {
                error?.let { Text(it, color = LumenColors.MistGray, fontSize = 13.sp); Spacer(Modifier.height(8.dp)) }
                Text("Refresh feed", color = LumenColors.FrostedBlue, modifier = Modifier.clickable(onClick = onRefresh))
                Text("Manage extensions", color = LumenColors.FrostedBlue,
                    modifier = Modifier.clickable(onClick = onOpenSources).padding(top = 8.dp))
            }
            else -> {
                Text("From your sources", color = LumenColors.MistGray.copy(alpha = 0.7f), fontSize = 11.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(feed, key = { it.id }) { series ->
                        Column(Modifier.fillMaxWidth().clickable { onOpenSeries(series) }) {
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(0.72f).clip(RoundedCornerShape(14.dp))
                                    .background(Brush.verticalGradient(listOf(Color(series.coverHint), LumenColors.DeepGraphite))),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                if (!series.coverUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = series.coverUrl,
                                        contentDescription = series.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Text(series.kind.uppercase(), color = LumenColors.FrostedBlue, fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(10.dp))
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(series.title, color = LumenColors.FrostWhite, fontSize = 13.sp,
                                fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(series.sourceName, color = LumenColors.MistGray.copy(alpha = 0.75f),
                                fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
