package com.lumen.reader.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SeriesChapter(
    val id: String,
    val title: String,
    val number: Int,
    val dateLabel: String = ""
)

data class CatalogSeries(
    val id: String,
    val title: String,
    val author: String,
    val sourceName: String,
    val kind: String,
    val description: String,
    val genres: List<String>,
    val status: String,
    val chapters: List<SeriesChapter>,
    val coverHint: Long = 0xFF1E2A3A
)

fun sampleCatalogFromInstalled(sourceName: String, kind: String): List<CatalogSeries> {
    val isManga = kind.equals("manga", ignoreCase = true) || kind.equals("MANGA", ignoreCase = true)
    return if (isManga) {
        listOf(
            CatalogSeries(
                id = "feed-$sourceName-1",
                title = "Global Martial Arts",
                author = "Studio Feed",
                sourceName = sourceName,
                kind = "manga",
                description = "Rebirth is just a starting point from reaching the peak.",
                genres = listOf("Action", "Adventure", "Manhua"),
                status = "Ongoing",
                chapters = (1..12).map { SeriesChapter("c$it", "Chapter $it", it, "Recent") }.reversed(),
                coverHint = 0xFF1E3A5F
            ),
            CatalogSeries(
                id = "feed-$sourceName-2",
                title = "Martial Peak",
                author = "Source catalog",
                sourceName = sourceName,
                kind = "manga",
                description = "Climb the martial path chapter by chapter.",
                genres = listOf("Action", "Fantasy"),
                status = "Ongoing",
                chapters = (1..8).map { SeriesChapter("c$it", "Chapter $it", it) }.reversed(),
                coverHint = 0xFF3A2A1E
            ),
            CatalogSeries(
                id = "feed-$sourceName-3",
                title = "One Piece",
                author = "Source catalog",
                sourceName = sourceName,
                kind = "manga",
                description = "Popular title surfaced from your installed extension.",
                genres = listOf("Adventure"),
                status = "Ongoing",
                chapters = (1..5).map { SeriesChapter("c$it", "Chapter $it", it) }.reversed(),
                coverHint = 0xFF1A3A4A
            )
        )
    } else {
        listOf(
            CatalogSeries(
                id = "feed-$sourceName-n1",
                title = "Quiet Hours",
                author = "Lumen Library",
                sourceName = sourceName,
                kind = "novel",
                description = "A soft morning light and pages that slow the day.",
                genres = listOf("Drama", "Slice of Life"),
                status = "Ongoing",
                chapters = (1..6).map { SeriesChapter("c$it", "Chapter $it", it) }.reversed(),
                coverHint = 0xFF2A3340
            ),
            CatalogSeries(
                id = "feed-$sourceName-n2",
                title = "Weekly Featured",
                author = "Source catalog",
                sourceName = sourceName,
                kind = "novel",
                description = "Featured novel entry from your installed LNReader plugin.",
                genres = listOf("Fantasy"),
                status = "Ongoing",
                chapters = (1..4).map { SeriesChapter("c$it", "Chapter $it", it) }.reversed(),
                coverHint = 0xFF3A2438
            )
        )
    }
}

@Composable
fun SeriesDetailScreen(
    series: CatalogSeries,
    onBack: () -> Unit,
    onStartChapter: (SeriesChapter) -> Unit,
    inLibrary: Boolean = false,
    onToggleLibrary: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LumenColors.SoftBlack)
    ) {
        // Soft ambient from cover hint
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(series.coverHint).copy(alpha = 0.45f),
                            LumenColors.SoftBlack
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.padding(top = 36.dp, start = 8.dp)
            ) {
                Text("← Back", color = LumenColors.FrostedBlue)
            }

            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(series.coverHint),
                                    LumenColors.DeepGraphite
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        series.title.take(1),
                        color = LumenColors.FrostWhite.copy(alpha = 0.7f),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        series.title,
                        color = LumenColors.FrostWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        series.author,
                        color = LumenColors.MistGray,
                        fontSize = 13.sp
                    )
                    Text(
                        "${series.status} · ${series.sourceName}",
                        color = LumenColors.MistGray.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (inLibrary) "In library" else "Add to library",
                            color = if (inLibrary) LumenColors.FrostedBlue else LumenColors.MistGray,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(LumenColors.DeepGraphite)
                                .clickable(onClick = onToggleLibrary)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                series.description,
                color = LumenColors.LiquidSilver.copy(alpha = 0.85f),
                fontSize = 13.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                series.genres.forEach { g ->
                    Text(
                        g,
                        color = LumenColors.MistGray,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(LumenColors.DeepGraphite)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "${series.chapters.size} chapters",
                color = LumenColors.FrostWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(series.chapters, key = { _, c -> c.id }) { _, ch ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onStartChapter(ch) }
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(LumenColors.FrostedBlue.copy(alpha = 0.7f))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ch.title, color = LumenColors.FrostWhite, fontSize = 14.sp)
                            if (ch.dateLabel.isNotBlank()) {
                                Text(
                                    ch.dateLabel,
                                    color = LumenColors.MistGray.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Text(
                            "↓",
                            color = LumenColors.MistGray,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Floating Start
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    "▶  Start",
                    color = LumenColors.SoftBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(LumenColors.FrostedBlue)
                        .clickable {
                            series.chapters.lastOrNull()?.let(onStartChapter)
                                ?: series.chapters.firstOrNull()?.let(onStartChapter)
                        }
                        .padding(horizontal = 22.dp, vertical = 14.dp)
                )
            }
        }
    }
}
