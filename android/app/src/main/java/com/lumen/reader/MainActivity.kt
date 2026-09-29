package com.lumen.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.lumen.reader.core.*
import com.lumen.reader.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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
    val libStore = remember { LibraryStore(context) }
    val updater = remember { AppUpdateManager(context) }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(Tab.Home) }
    var sourcesOpen by remember { mutableStateOf(false) }
    var authOpen by remember { mutableStateOf(false) }
    var downloadsOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var creatorOpen by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<CatalogSeries?>(null) }
    var reading by remember { mutableStateOf<SampleSeries?>(null) }
    var library by remember { mutableStateOf(libStore.loadLibrary()) }
    var history by remember { mutableStateOf(libStore.loadHistory()) }
    var installTick by remember { mutableStateOf(0) }
    var installed by remember { mutableStateOf(store.getInstalled()) }
    var feed by remember { mutableStateOf<List<CatalogSeries>>(emptyList()) }
    var feedLoading by remember { mutableStateOf(false) }
    var feedProgress by remember { mutableStateOf("") }
    var feedError by remember { mutableStateOf<String?>(null) }
    val updateState by updater.state.collectAsState()

    fun refreshInstalled() {
        installed = store.getInstalled().filter { e ->
            if (e.pkg.isNullOrBlank()) true
            else installer.isPackageInstalled(e.pkg) || store.isInstalled(e.id)
        }
    }

    fun toCatalog(ls: CatalogService.LiveSeries) = CatalogSeries(
        id = ls.id, title = ls.title,
        author = ls.author.ifBlank { ls.sourceName },
        sourceName = ls.sourceName, kind = ls.kind,
        description = ls.description,
        genres = ls.genres.ifEmpty { listOf(ls.sourceName) },
        status = "Ongoing", chapters = emptyList(),
        coverHint = 0xFF1E2A3A, coverUrl = ls.coverUrl,
        seriesUrl = ls.url, sourceId = ls.sourceId
    )

    fun loadFeed() {
        scope.launch {
            feedLoading = true
            feedError = null
            feedProgress = "Starting\u2026"
            refreshInstalled()
            val withSite = installed.filter {
                (!it.site.isNullOrBlank() && it.site!!.startsWith("http")) || !it.pkg.isNullOrBlank()
            }
            val sources = withSite.ifEmpty { installed }.take(16)
            val seen = linkedMapOf<String, CatalogSeries>()

            feedProgress = "Lumen catalog\u2026"
            runCatching { com.lumen.reader.data.SupabaseClient.fetchLumenFeed(24) }
                .getOrDefault(emptyList())
                .forEach { s ->
                    val c = CatalogSeries(
                        id = "lumen_" + s.id, title = s.title, author = "Lumen",
                        sourceName = "Lumen",
                        kind = if (s.contentType.contains("novel", true)) "novel" else "manga",
                        description = s.description, genres = listOf("Lumen"),
                        status = "Ongoing", chapters = emptyList(),
                        coverHint = 0xFF1E2A3A, coverUrl = s.coverUrl,
                        seriesUrl = null, sourceId = "lumen"
                    )
                    seen[c.id] = c
                }
            if (seen.isNotEmpty()) {
                feed = seen.values.toList()
                feedLoading = false
            }

            if (sources.isEmpty()) {
                if (feed.isEmpty()) feedError = "Install extensions to fill this feed"
                feedLoading = false
                feedProgress = ""
                return@launch
            }

            for ((idx, entry) in sources.withIndex()) {
                feedProgress = "${entry.name} (${idx + 1}/${sources.size})"
                feedLoading = true
                val batch = withContext(Dispatchers.IO) {
                    withTimeoutOrNull(10_000L) {
                        runCatching {
                            runtime.fetchAllPopular(listOf(entry), perSource = 16, maxConcurrent = 1)
                        }.getOrDefault(emptyList())
                    } ?: emptyList()
                }
                for (ls in batch) {
                    val c = toCatalog(ls)
                    if (!seen.containsKey(c.id)) seen[c.id] = c
                }
                feed = seen.values.toList()
                if (feed.isNotEmpty()) feedLoading = false
            }

            if (feed.isEmpty() && sources.isNotEmpty()) {
                feedError = "No listings yet \u2014 refresh or check Sources."
            }
            feedLoading = false
            feedProgress = ""
        }
    }

    LaunchedEffect(installTick) { loadFeed() }
    LaunchedEffect(Unit) {
        val u = com.lumen.reader.data.SupabaseClient.refreshProfileUsername()
        if (!u.isNullOrBlank()) {
            com.lumen.reader.data.AuthSession.markSignedIn(
                context, com.lumen.reader.data.AuthSession.email(context), u
            )
        }
    }

    BackHandler(enabled = authOpen || sourcesOpen || settingsOpen || creatorOpen || downloadsOpen || detail != null || reading != null) {
        when {
            reading != null -> reading = null
            detail != null -> detail = null
            creatorOpen -> creatorOpen = false
            settingsOpen -> settingsOpen = false
            downloadsOpen -> downloadsOpen = false
            authOpen -> authOpen = false
            sourcesOpen -> { sourcesOpen = false; installTick++ }
        }
    }

    if (reading != null) {
        ReaderScreen(series = reading!!, onClose = { reading = null })
        return
    }
    if (detail != null) {
        val d = detail!!
        SeriesDetailScreen(
            series = d, onBack = { detail = null },
            inLibrary = library.any { it.id == d.id },
            onToggleLibrary = {
                if (library.any { it.id == d.id }) libStore.removeFromLibrary(d.id)
                else libStore.addToLibrary(d)
                library = libStore.loadLibrary()
            },
            onStartChapter = { ch ->
                libStore.pushHistory(d)
                history = libStore.loadHistory()
                scope.launch {
                    val isNovel = d.kind.equals("novel", ignoreCase = true)
                    val cached = dlStore.loadChapter(ch.id)
                    val pages = cached ?: withContext(Dispatchers.IO) {
                        CatalogService.fetchChapterPages(ch.id, isNovel)
                    }
                    reading = if (isNovel) {
                        SAMPLE_NOVEL.copy(
                            id = d.id, title = d.title, author = d.author,
                            chapterTitle = ch.title, chapterId = ch.id,
                            pages = pages.ifEmpty { listOf("No text extracted.") }
                        )
                    } else {
                        SAMPLE_MANGA.copy(
                            id = d.id, title = d.title, author = d.author,
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
    if (downloadsOpen) {
        DownloadsScreen(onBack = { downloadsOpen = false })
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
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.Home -> HomeGrid(
                    feed, feedLoading, feedProgress, feedError, installed.size,
                    { sourcesOpen = true },
                    { s -> openDetail(s, installed, runtime, scope) { detail = it } },
                    { loadFeed() }
                )
                Tab.Library -> SeriesShelf(
                    title = "Library",
                    empty = "Add titles from Home or series detail.",
                    items = library,
                    onOpen = { s -> openDetail(s, installed, runtime, scope) { detail = it } }
                )
                Tab.History -> SeriesShelf(
                    title = "History",
                    empty = "Nothing read yet.",
                    items = history,
                    onOpen = { s -> openDetail(s, installed, runtime, scope) { detail = it } }
                )
                Tab.Updates -> UpdatesTab(
                    state = updateState,
                    onCheck = { scope.launch { updater.checkForUpdate() } },
                    onDownload = { scope.launch { updater.downloadAndInstall() } },
                    onOpenSources = { sourcesOpen = true }
                )
                Tab.More -> MoreScreen(
                    onSignIn = { authOpen = true },
                    onOpenSources = { sourcesOpen = true },
                    onOpenDownloads = { downloadsOpen = true },
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
