package com.lumen.reader.ui

import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.reader.core.ChapterSocialStore
import com.lumen.reader.core.ChapterVote
import java.util.Locale

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
    val kind: String,
    val chapterTitle: String,
    val pages: List<String>,
    val ambientHints: List<Long>,
    val chapterId: String = "ch-1"
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
fun ReaderScreen(series: SampleSeries, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val social = remember { ChapterSocialStore(context) }
    var pageIndex by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    var chromeVisible by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var showChapters by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }
    var fontScale by remember { mutableFloatStateOf(1f) }
    var ttsOn by remember { mutableStateOf(false) }
    var vote by remember { mutableStateOf(social.getVote(series.id, series.chapterId)) }
    var likes by remember { mutableIntStateOf(social.likeCount(series.id, series.chapterId)) }
    var dislikes by remember { mutableIntStateOf(social.dislikeCount(series.id, series.chapterId)) }
    var comments by remember { mutableStateOf(social.getComments(series.id, series.chapterId)) }
    var draft by remember { mutableStateOf("") }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) engine?.language = Locale.US
        }
        tts = engine
        onDispose { engine?.stop(); engine?.shutdown() }
    }

    val ambient = Color(series.ambientHints.getOrElse(pageIndex) { 0xFF0A0C0F })
    LaunchedEffect(pageIndex) {
        progress = (pageIndex + 1).toFloat() / series.pages.size.coerceAtLeast(1)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LumenColors.SoftBlack)
            .clickable { chromeVisible = !chromeVisible }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (chromeVisible) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onClose) { Text("\u2190", color = LumenColors.FrostedBlue) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(series.title, color = LumenColors.FrostWhite, fontSize = 13.sp, maxLines = 1)
                        Text(series.chapterTitle, color = LumenColors.MistGray, fontSize = 11.sp, maxLines = 1)
                    }
                    TextButton(onClick = { showSettings = true }) {
                        Text("\u22EE", color = LumenColors.FrostWhite, fontSize = 20.sp)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (series.kind == "novel") {
                    val scroll = rememberScrollState()
                    LaunchedEffect(scroll.value, scroll.maxValue) {
                        if (scroll.maxValue > 0) progress = scroll.value.toFloat() / scroll.maxValue
                    }
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(22.dp)
                    ) {
                        Text(series.chapterTitle, color = LumenColors.FrostWhite, fontSize = (22 * fontScale).sp, fontWeight = FontWeight.SemiBold)
                        series.pages.forEach { para ->
                            Text(
                                para,
                                color = LumenColors.LiquidSilver,
                                fontSize = (17 * fontScale).sp,
                                lineHeight = (28 * fontScale).sp,
                                fontFamily = FontFamily.Serif,
                                modifier = Modifier.padding(bottom = 18.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.verticalGradient(listOf(ambient.copy(alpha = 0.5f), LumenColors.DeepGraphite)))
                            .pointerInput(pageIndex) {
                                detectHorizontalDragGestures { _, drag ->
                                    if (drag < -40 && pageIndex < series.pages.lastIndex) pageIndex += 1
                                    if (drag > 40 && pageIndex > 0) pageIndex -= 1
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(series.pages[pageIndex], color = LumenColors.FrostWhite, fontSize = 22.sp)
                            Text("Swipe \u00b7 ${pageIndex + 1} / ${series.pages.size}", color = LumenColors.MistGray, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (chromeVisible) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.55f)).padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(progress.coerceIn(0.02f, 1f)).height(3.dp).background(LumenColors.FrostedBlue)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (ttsOn) "Stop" else "TTS",
                            color = LumenColors.FrostedBlue,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                if (ttsOn) {
                                    tts?.stop(); ttsOn = false
                                } else {
                                    val spoken = if (series.kind == "novel") series.pages.joinToString(" ") else series.pages[pageIndex]
                                    tts?.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "lumen-tts")
                                    ttsOn = true
                                }
                            }.padding(8.dp)
                        )
                        Text("Chapters", color = LumenColors.FrostedBlue, fontSize = 12.sp, modifier = Modifier.clickable { showChapters = true }.padding(8.dp))
                        Text(
                            if (vote == ChapterVote.LIKE) "\u25B2 $likes" else "\u25B3 $likes",
                            color = if (vote == ChapterVote.LIKE) LumenColors.FrostedBlue else LumenColors.MistGray,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                social.setVote(series.id, series.chapterId, ChapterVote.LIKE)
                                vote = social.getVote(series.id, series.chapterId)
                                likes = social.likeCount(series.id, series.chapterId)
                                dislikes = social.dislikeCount(series.id, series.chapterId)
                            }.padding(8.dp)
                        )
                        Text(
                            if (vote == ChapterVote.DISLIKE) "\u25BC $dislikes" else "\u25BD $dislikes",
                            color = if (vote == ChapterVote.DISLIKE) Color(0xFFF87171) else LumenColors.MistGray,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                social.setVote(series.id, series.chapterId, ChapterVote.DISLIKE)
                                vote = social.getVote(series.id, series.chapterId)
                                likes = social.likeCount(series.id, series.chapterId)
                                dislikes = social.dislikeCount(series.id, series.chapterId)
                            }.padding(8.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Comments", color = LumenColors.FrostedBlue, fontSize = 12.sp, modifier = Modifier.clickable { showComments = !showComments }.padding(8.dp))
                    }
                }
            }
        }

        if (showComments) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(220.dp).background(LumenColors.DeepGraphite).padding(12.dp)
            ) {
                Text("Comments", color = LumenColors.FrostWhite, fontSize = 13.sp)
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    if (comments.isEmpty()) Text("No comments yet.", color = LumenColors.MistGray, fontSize = 12.sp)
                    comments.forEach { c ->
                        Text(c.author, color = LumenColors.FrostedBlue, fontSize = 11.sp)
                        Text(c.body, color = LumenColors.LiquidSilver, fontSize = 13.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Write a comment\u2026", color = LumenColors.MistGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LumenColors.FrostedBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = LumenColors.FrostedBlue
                        )
                    )
                    TextButton(onClick = {
                        if (draft.isNotBlank()) {
                            social.addComment(series.id, series.chapterId, "You", draft)
                            comments = social.getComments(series.id, series.chapterId)
                            draft = ""
                        }
                    }) { Text("Post", color = LumenColors.FrostedBlue) }
                }
            }
        }

        if (showSettings) {
            Column(
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.9f).clip(RoundedCornerShape(20.dp)).background(LumenColors.DeepGraphite).padding(20.dp)
            ) {
                Text("Reading settings", color = LumenColors.FrostWhite, fontWeight = FontWeight.SemiBold)
                Text("Text size", color = LumenColors.MistGray, fontSize = 12.sp)
                Slider(value = fontScale, onValueChange = { fontScale = it }, valueRange = 0.85f..1.4f)
                Text(if (series.kind == "novel") "Mode: infinite scroll" else "Mode: swipe pages", color = LumenColors.MistGray, fontSize = 12.sp)
                TextButton(onClick = { showSettings = false }) { Text("Done", color = LumenColors.FrostedBlue) }
            }
        }

        if (showChapters) {
            Column(
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.9f).clip(RoundedCornerShape(20.dp)).background(LumenColors.DeepGraphite).padding(16.dp)
            ) {
                Text("Chapters", color = LumenColors.FrostWhite, fontWeight = FontWeight.SemiBold)
                Text(series.chapterTitle, color = LumenColors.FrostedBlue, fontSize = 13.sp)
                TextButton(onClick = { showChapters = false }) { Text("Close", color = LumenColors.FrostedBlue) }
            }
        }
    }
}
