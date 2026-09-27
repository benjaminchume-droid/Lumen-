package com.lumen.reader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SeriesChapter(val id: String, val title: String, val number: Int, val dateLabel: String = "")

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
    val coverHint: Long = 0xFF1E2A3A,
    val coverUrl: String? = null,
    val seriesUrl: String? = null,
    val sourceId: String = ""
)

fun sampleCatalogFromInstalled(sourceName: String, kind: String): List<CatalogSeries> {
    // No demo catalog — home only shows real installed extension packages
    return emptyList()
}

@Composable
fun SeriesDetailScreen(
    series: CatalogSeries,
    onBack: () -> Unit,
    inLibrary: Boolean,
    onToggleLibrary: () -> Unit,
    onStartChapter: (SeriesChapter) -> Unit,
    onDownloadChapters: (List<SeriesChapter>) -> Unit = {}
) {
    BackHandler(onBack = onBack)
    var showBulk by remember { mutableStateOf(false) }
    val noRipple = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxSize().background(LumenColors.SoftBlack)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp).background(
                Brush.verticalGradient(listOf(Color(series.coverHint).copy(alpha = 0.45f), LumenColors.SoftBlack))
            )
        )
        Column(modifier = Modifier.fillMaxSize()) {
            TextButton(onClick = onBack, modifier = Modifier.padding(top = 36.dp, start = 4.dp)) {
                Text("←", color = LumenColors.FrostedBlue, fontSize = 20.sp)
            }
            Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(width = 110.dp, height = 150.dp).clip(RoundedCornerShape(12.dp))
                        .background(Brush.verticalGradient(listOf(Color(series.coverHint), LumenColors.DeepGraphite))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(series.title.take(1), color = LumenColors.FrostWhite.copy(alpha = 0.7f), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(series.title, color = LumenColors.FrostWhite, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(series.author, color = LumenColors.MistGray, fontSize = 13.sp)
                    Text("${series.sourceName} · ${series.kind}", color = LumenColors.MistGray.copy(alpha = 0.7f), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (inLibrary) "In library" else "Add to library",
                            color = if (inLibrary) LumenColors.FrostedBlue else LumenColors.SoftBlack,
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                .background(if (inLibrary) Color(0x227BC6FF) else LumenColors.FrostedBlue)
                                .clickable(interactionSource = noRipple, indication = null, onClick = onToggleLibrary)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                        Text(
                            "Start", color = LumenColors.SoftBlack, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(LumenColors.FrostedBlue)
                                .clickable(interactionSource = noRipple, indication = null) {
                                    series.chapters.lastOrNull()?.let(onStartChapter)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (series.description.isNotBlank()) {
                Text(
                    series.description,
                    color = LumenColors.MistGray.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${series.chapters.size} chapters", color = LumenColors.FrostWhite, fontWeight = FontWeight.Medium)
                Text(
                    "Download",
                    color = LumenColors.FrostedBlue,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { showBulk = !showBulk }
                )
            }
            if (showBulk) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 25, 50, 100).forEach { n ->
                        Text(
                            "$n",
                            color = LumenColors.SoftBlack,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(LumenColors.FrostedBlue)
                                .clickable {
                                    onDownloadChapters(series.chapters.take(n))
                                    showBulk = false
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Text(
                        "All",
                        color = LumenColors.SoftBlack,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(LumenColors.FrostedBlue)
                            .clickable {
                                onDownloadChapters(series.chapters)
                                showBulk = false
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                itemsIndexed(series.chapters) { _, ch ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(interactionSource = noRipple, indication = null) { onStartChapter(ch) }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ch.title, color = LumenColors.FrostWhite, fontSize = 14.sp)
                            if (ch.dateLabel.isNotBlank()) {
                                Text(ch.dateLabel, color = LumenColors.MistGray, fontSize = 11.sp)
                            }
                        }
                        IconButton(onClick = { onDownloadChapters(listOf(ch)) }) {
                            Icon(Icons.Default.Download, contentDescription = "Download", tint = LumenColors.MistGray, modifier = Modifier.size(18.dp))
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = "Read", tint = LumenColors.FrostedBlue, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}
