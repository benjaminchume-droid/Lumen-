package com.lumen.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lumen.reader.BuildConfig
import com.lumen.reader.core.*
import com.lumen.reader.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SeriesShelf(
    title: String,
    empty: String,
    items: List<CatalogSeries>,
    onOpen: (CatalogSeries) -> Unit
) {
    Column(Modifier.fillMaxSize().background(LumenColors.SoftBlack).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(48.dp))
        Text(title, color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text("${items.size} titles", color = LumenColors.MistGray, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        if (items.isEmpty()) {
            Text(empty, color = LumenColors.MistGray, fontSize = 13.sp)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.id }) { series ->
                    SeriesCoverCard(series) { onOpen(series) }
                }
            }
        }
    }
}

@Composable
fun SeriesCoverCard(series: CatalogSeries, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
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
            } else {
                Text(
                    series.title.take(1).uppercase(),
                    color = LumenColors.FrostWhite.copy(0.5f),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Text(
                series.kind.uppercase(),
                color = LumenColors.FrostedBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(10.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            series.title, color = LumenColors.FrostWhite, fontSize = 13.sp,
            fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis
        )
        Text(
            series.sourceName, color = LumenColors.MistGray.copy(alpha = 0.75f),
            fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun UpdatesTab(
    state: UpdateState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onOpenSources: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(LumenColors.SoftBlack).padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        Text("Updates", color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text("App version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = LumenColors.MistGray, fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))

        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(LumenColors.DeepGraphite).padding(16.dp)
        ) {
            Column {
                Text(
                    when (state.phase) {
                        UpdatePhase.IDLE -> "Tap check for the latest GitHub release"
                        UpdatePhase.CHECKING -> "Checking GitHub releases\u2026"
                        UpdatePhase.AVAILABLE -> "Update available: ${state.remoteTag}"
                        UpdatePhase.DOWNLOADING -> "Downloading\u2026 ${state.percent}%"
                        UpdatePhase.READY, UpdatePhase.INSTALLING -> state.message.ifBlank { "Ready to install" }
                        UpdatePhase.UP_TO_DATE -> state.message.ifBlank { "You're on the latest version" }
                        UpdatePhase.FAILED -> state.message.ifBlank { "Check failed" }
                    },
                    color = LumenColors.FrostWhite,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                if (state.message.isNotBlank() && state.phase != UpdatePhase.IDLE) {
                    Spacer(Modifier.height(6.dp))
                    Text(state.message, color = LumenColors.MistGray, fontSize = 12.sp)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (state.phase == UpdatePhase.CHECKING) "Checking\u2026" else "Check for updates",
                        color = LumenColors.SoftBlack,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(LumenColors.FrostedBlue)
                            .clickable(
                                enabled = state.phase != UpdatePhase.CHECKING && state.phase != UpdatePhase.DOWNLOADING,
                                onClick = onCheck
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                    if (state.phase == UpdatePhase.AVAILABLE || state.phase == UpdatePhase.READY) {
                        Text(
                            if (state.phase == UpdatePhase.DOWNLOADING) "Downloading\u2026" else "Download \u0026 install",
                            color = LumenColors.FrostWhite,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2563EB))
                                .clickable(onClick = onDownload)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
                if (state.phase == UpdatePhase.DOWNLOADING && state.bytesTotal > 0) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { state.percent / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = LumenColors.FrostedBlue,
                        trackColor = Color(0xFF1A2030)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Extension sources",
            color = LumenColors.FrostedBlue,
            modifier = Modifier.clickable(onClick = onOpenSources).padding(vertical = 8.dp)
        )
        Text("Manage Keiyoushi / LNReader packages", color = LumenColors.MistGray, fontSize = 12.sp)
    }
}

/**
 * Opens series detail via SourceRegistry only (Mihon → LN → fallback).
 */
fun openDetail(
    series: CatalogSeries,
    registry: SourceRegistry,
    scope: kotlinx.coroutines.CoroutineScope,
    setDetail: (CatalogSeries) -> Unit
) {
    setDetail(series)
    val sourceId = series.sourceId ?: return
    val seriesUrl = series.seriesUrl ?: return
    if (sourceId == "lumen") return
    scope.launch {
        val details = withContext(Dispatchers.IO) {
            runCatching { registry.details(sourceId, seriesUrl) }.getOrNull()
        }
        val chapters = withContext(Dispatchers.IO) {
            runCatching { registry.chapters(sourceId, seriesUrl) }.getOrDefault(emptyList())
        }
        setDetail(
            series.copy(
                title = details?.title?.takeIf { it.isNotBlank() } ?: series.title,
                author = details?.author?.takeIf { it.isNotBlank() } ?: series.author,
                description = details?.description?.takeIf { it.isNotBlank() } ?: series.description,
                genres = details?.genres?.takeIf { it.isNotEmpty() } ?: series.genres,
                coverUrl = details?.coverUrl?.takeIf { it.isNotBlank() } ?: series.coverUrl,
                chapters = chapters.mapIndexed { i, c ->
                    SeriesChapter(
                        id = c.url,
                        title = c.name,
                        number = c.number.toInt().coerceAtLeast(i + 1)
                    )
                }
            )
        )
    }
}

@Composable
fun HomeGrid(
    feed: List<CatalogSeries>,
    loading: Boolean,
    progress: String,
    error: String?,
    installedCount: Int,
    onOpenSources: () -> Unit,
    onOpenSeries: (CatalogSeries) -> Unit,
    onRefresh: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(LumenColors.SoftBlack).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(48.dp))
        Text("Home", color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text(
            when {
                loading && feed.isEmpty() -> progress.ifBlank { "Loading first source\u2026" }
                loading -> "${feed.size} titles \u00b7 still loading $progress"
                installedCount == 0 && feed.isEmpty() -> "Install extensions to fill this feed"
                feed.isEmpty() -> "$installedCount source(s) \u00b7 no listings yet"
                else -> "${feed.size} titles from $installedCount source(s)"
            },
            color = LumenColors.MistGray.copy(alpha = 0.75f), fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))
        when {
            loading && feed.isEmpty() -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(6) {
                        Column(Modifier.fillMaxWidth()) {
                            Box(Modifier.fillMaxWidth().aspectRatio(0.72f).clip(RoundedCornerShape(14.dp)).background(Color(0xFF1A2030)))
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.fillMaxWidth(0.8f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF1A2030)))
                        }
                    }
                }
            }
            installedCount == 0 && feed.isEmpty() -> Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(LumenColors.DeepGraphite).padding(20.dp)
            ) {
                Column {
                    Text("No extensions yet", color = LumenColors.FrostWhite, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Text("Open Sources to install Keiyoushi / LNReader extensions.", color = LumenColors.MistGray, fontSize = 13.sp)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Install extensions", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(LumenColors.FrostedBlue)
                            .clickable(onClick = onOpenSources).padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
            feed.isEmpty() -> Column {
                error?.let { Text(it, color = LumenColors.MistGray, fontSize = 13.sp); Spacer(Modifier.height(8.dp)) }
                Text("Refresh feed", color = LumenColors.FrostedBlue, modifier = Modifier.clickable(onClick = onRefresh))
                Text("Manage extensions", color = LumenColors.FrostedBlue, modifier = Modifier.clickable(onClick = onOpenSources).padding(top = 8.dp))
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(feed, key = { it.id }) { series ->
                        SeriesCoverCard(series) { onOpenSeries(series) }
                    }
                }
            }
        }
    }
}
