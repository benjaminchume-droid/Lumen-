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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Brand palette — Frost White, Liquid Silver, Mist Gray, Deep Graphite, Soft Black, Frosted Blue */
object LumenColors {
    val FrostWhite = Color(0xFFF5F7FA)
    val LiquidSilver = Color(0xFFD9E1EA)
    val MistGray = Color(0xFF94A3B8)
    val DeepGraphite = Color(0xFF12151C)
    val SoftBlack = Color(0xFF0A0C0F)
    val FrostedBlue = Color(0xFF7BC6FF)
}

data class SampleSeries(
    val id: String,
    val title: String,
    val author: String,
    val kind: String, // novel | manga
    val chapterTitle: String,
    val pages: List<String>, // novel paragraphs OR manga page labels
    val ambientHints: List<Long> // ARGB colors derived per page for ambient lighting
)

val SAMPLE_NOVEL = SampleSeries(
    id = "sample-novel",
    title = "Quiet Hours",
    author = "Lumen Library",
    kind = "novel",
    chapterTitle = "Chapter 1 — Morning light",
    pages = listOf(
        "The room held a soft gray light, the kind that arrives before the day decides what it will be.",
        "She opened the book again, not for the plot, but for the way the sentences slowed her breathing.",
        "Outside, rain moved across the glass in thin lines. Inside, the page stayed warm under her hands.",
        "Nothing urgent waited. Only the next paragraph, and the quiet between words."
    ),
    ambientHints = listOf(0xFF1B2838, 0xFF243044, 0xFF1A2330, 0xFF2A3340)
)

val SAMPLE_MANGA = SampleSeries(
    id = "sample-manga",
    title = "Frame by Frame",
    author = "Lumen Library",
    kind = "manga",
    chapterTitle = "Chapter 1 — First panels",
    pages = listOf("Page 1", "Page 2", "Page 3", "Page 4", "Page 5"),
    ambientHints = listOf(0xFF1E2A3A, 0xFF2C1F2E, 0xFF1A2E28, 0xFF2A2418, 0xFF1C2430)
)

@Composable
fun ReaderScreen(
    series: SampleSeries,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    var pageIndex by remember { mutableStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }

    val ambient = Color(series.ambientHints.getOrElse(pageIndex) { 0xFF0A0C0F })
    val bg = blendTowardSoftBlack(ambient, 0.72f)

    LaunchedEffect(pageIndex) {
        progress = (pageIndex + 1).toFloat() / series.pages.size.coerceAtLeast(1)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        ambient.copy(alpha = 0.35f),
                        bg,
                        LumenColors.SoftBlack
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar — faint labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onClose) {
                    Text("← Back", color = LumenColors.FrostedBlue, fontSize = 13.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        series.kind.uppercase(),
                        color = LumenColors.FrostedBlue.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "${series.title} — ${series.chapterTitle}",
                        color = LumenColors.LiquidSilver.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            if (series.kind == "novel") {
                NovelBody(
                    series = series,
                    onScrollProgress = { progress = it }
                )
            } else {
                MangaBody(
                    series = series,
                    pageIndex = pageIndex,
                    onPage = { pageIndex = it }
                )
            }

            // Bottom progress
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = LumenColors.FrostedBlue,
                    trackColor = Color.White.copy(alpha = 0.08f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "${(progress * 100).toInt()}% · ${series.pages.size} sections",
                    color = LumenColors.MistGray.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun NovelBody(series: SampleSeries, onScrollProgress: (Float) -> Unit) {
    val scroll = rememberScrollState()
    LaunchedEffect(scroll.value, scroll.maxValue) {
        if (scroll.maxValue > 0) {
            onScrollProgress(scroll.value.toFloat() / scroll.maxValue)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(scroll)
            .padding(horizontal = 22.dp, vertical = 8.dp)
    ) {
        Text(
            series.chapterTitle,
            color = LumenColors.FrostWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "By ${series.author}",
            color = LumenColors.MistGray.copy(alpha = 0.8f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
        )
        series.pages.forEach { para ->
            Text(
                para,
                color = LumenColors.LiquidSilver.copy(alpha = 0.92f),
                fontSize = 17.sp,
                lineHeight = 28.sp,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            "End of chapter",
            color = LumenColors.MistGray.copy(alpha = 0.55f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun MangaBody(
    series: SampleSeries,
    pageIndex: Int,
    onPage: (Int) -> Unit
) {
    val pager = rememberPagerState(pageCount = { series.pages.size })
    LaunchedEffect(pager.currentPage) { onPage(pager.currentPage) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = 16.dp)
    ) {
        HorizontalPager(
            state = pager,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            val tint = Color(series.ambientHints[page])
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(tint.copy(alpha = 0.55f), LumenColors.DeepGraphite)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        series.pages[page],
                        color = LumenColors.FrostWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Swipe for next page",
                        color = LumenColors.MistGray.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
        }
        Text(
            "Page ${pageIndex + 1} of ${series.pages.size}",
            color = LumenColors.MistGray.copy(alpha = 0.65f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(8.dp)
        )
    }
}

private fun blendTowardSoftBlack(c: Color, amount: Float): Color {
    val a = amount.coerceIn(0f, 1f)
    return Color(
        red = c.red * (1 - a),
        green = c.green * (1 - a),
        blue = c.blue * (1 - a),
        alpha = 1f
    )
}
