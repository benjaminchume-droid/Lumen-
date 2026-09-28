package com.lumen.reader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.reader.data.AuthSession
import com.lumen.reader.data.SupabaseClient
import kotlinx.coroutines.launch

enum class CreatorView { Hub, NovelForm, ComicForm, Done }

@Composable
fun CreatorStudioScreen(onBack: () -> Unit, onNeedAuth: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val signedIn = AuthSession.isSignedIn(context)
    val username = AuthSession.username(context).ifBlank { AuthSession.email(context).substringBefore("@") }
    var view by remember { mutableStateOf(CreatorView.Hub) }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    var novelTitle by remember { mutableStateOf("") }
    var novelDesc by remember { mutableStateOf("") }
    var novelCover by remember { mutableStateOf("") }
    var novelGenres by remember { mutableStateOf("") }
    var novelChapter by remember { mutableStateOf("") }

    var comicTitle by remember { mutableStateOf("") }
    var comicDesc by remember { mutableStateOf("") }
    var comicCover by remember { mutableStateOf("") }
    var comicGenres by remember { mutableStateOf("") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = LumenColors.FrostedBlue,
        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        cursorColor = LumenColors.FrostedBlue,
        focusedContainerColor = LumenColors.DeepGraphite,
        unfocusedContainerColor = LumenColors.DeepGraphite
    )

    Column(
        Modifier.fillMaxSize().background(LumenColors.SoftBlack).verticalScroll(rememberScrollState())
            .imePadding().padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(40.dp))
        TextButton(onClick = {
            when (view) {
                CreatorView.Hub, CreatorView.Done -> onBack()
                else -> view = CreatorView.Hub
            }
            status = null
        }) { Text("← Back", color = LumenColors.FrostedBlue) }

        when (view) {
            CreatorView.Hub -> {
                Text("Creator Studio", color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Text(if (signedIn) "Publishing as @$username" else "Sign in to publish", color = LumenColors.MistGray, fontSize = 13.sp)
                Spacer(Modifier.height(20.dp))
                if (!signedIn) {
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(Color(0x33F59E0B))
                            .border(1.dp, Color(0x55F59E0B), RoundedCornerShape(16.dp))
                            .clickable(onClick = onNeedAuth).padding(16.dp)
                    ) {
                        Column {
                            Text("Sign in required", color = Color(0xFFFBBF24), fontWeight = FontWeight.SemiBold)
                            Text("Tap to sign in — then publish novels & comics to the public feed.", color = LumenColors.MistGray, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                CreatorCard("Create a novel", "Title, description, cover URL, first chapter text", signedIn) {
                    if (signedIn) view = CreatorView.NovelForm else onNeedAuth()
                }
                Spacer(Modifier.height(12.dp))
                CreatorCard("Create a comic / manga", "Title, cover, genres — chapter images coming soon", signedIn) {
                    if (signedIn) view = CreatorView.ComicForm else onNeedAuth()
                }
                Spacer(Modifier.height(12.dp))
                CreatorCard("Import from file", "EPUB / CBZ upload", false, locked = true) {}
            }
            CreatorView.NovelForm -> {
                Text("New novel", color = LumenColors.FrostWhite, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Text("Posted works appear on everyone’s Home feed (Lumen origin).", color = LumenColors.MistGray, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(novelTitle, { novelTitle = it }, Modifier.fillMaxWidth(),
                    label = { Text("Title", color = LumenColors.MistGray) }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = fieldColors)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(novelCover, { novelCover = it }, Modifier.fillMaxWidth(),
                    label = { Text("Cover image URL", color = LumenColors.MistGray) }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = fieldColors)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(novelGenres, { novelGenres = it }, Modifier.fillMaxWidth(),
                    label = { Text("Genres (comma separated)", color = LumenColors.MistGray) }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = fieldColors)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(novelDesc, { novelDesc = it }, Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    label = { Text("Description / synopsis", color = LumenColors.MistGray) },
                    shape = RoundedCornerShape(12.dp), colors = fieldColors)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(novelChapter, { novelChapter = it }, Modifier.fillMaxWidth().heightIn(min = 160.dp),
                    label = { Text("First chapter text", color = LumenColors.MistGray) },
                    shape = RoundedCornerShape(12.dp), colors = fieldColors)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    if (novelTitle.isBlank()) { status = "Title is required"; return@Button }
                    if (novelChapter.isBlank()) { status = "Add first chapter text"; return@Button }
                    busy = true; status = null
                    scope.launch {
                        val ok = SupabaseClient.publishSeries(
                            title = novelTitle.trim(), description = novelDesc.trim(),
                            coverUrl = novelCover.trim().ifBlank { null }, contentType = "novel",
                            genres = novelGenres.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                            firstChapterTitle = "Chapter 1", firstChapterText = novelChapter.trim()
                        )
                        busy = false
                        if (ok) { status = "Published! Visible on the public feed."; view = CreatorView.Done }
                        else status = SupabaseClient.lastError ?: "Publish failed — check connection"
                    }
                }, enabled = !busy && signedIn,
                    colors = ButtonDefaults.buttonColors(containerColor = LumenColors.FrostedBlue),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text(if (busy) "Publishing…" else "Publish novel", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold) }
            }
            CreatorView.ComicForm -> {
                Text("New comic / manga", color = LumenColors.FrostWhite, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(comicTitle, { comicTitle = it }, Modifier.fillMaxWidth(),
                    label = { Text("Title", color = LumenColors.MistGray) }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = fieldColors)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(comicCover, { comicCover = it }, Modifier.fillMaxWidth(),
                    label = { Text("Cover image URL", color = LumenColors.MistGray) }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = fieldColors)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(comicGenres, { comicGenres = it }, Modifier.fillMaxWidth(),
                    label = { Text("Genres", color = LumenColors.MistGray) }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = fieldColors)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(comicDesc, { comicDesc = it }, Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    label = { Text("Description", color = LumenColors.MistGray) },
                    shape = RoundedCornerShape(12.dp), colors = fieldColors)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    if (comicTitle.isBlank()) { status = "Title is required"; return@Button }
                    busy = true; status = null
                    scope.launch {
                        val ok = SupabaseClient.publishSeries(
                            title = comicTitle.trim(), description = comicDesc.trim(),
                            coverUrl = comicCover.trim().ifBlank { null }, contentType = "manga",
                            genres = comicGenres.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                            firstChapterTitle = null, firstChapterText = null
                        )
                        busy = false
                        if (ok) { status = "Published! Visible on the public feed."; view = CreatorView.Done }
                        else status = SupabaseClient.lastError ?: "Publish failed"
                    }
                }, enabled = !busy && signedIn,
                    colors = ButtonDefaults.buttonColors(containerColor = LumenColors.FrostedBlue),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text(if (busy) "Publishing…" else "Publish comic", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold) }
            }
            CreatorView.Done -> {
                Text("Published", color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Text("Your work is on the Lumen catalog for other readers.", color = LumenColors.MistGray, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { view = CreatorView.Hub; status = null },
                    colors = ButtonDefaults.buttonColors(containerColor = LumenColors.FrostedBlue),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text("Create another", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onBack) { Text("Back to app", color = LumenColors.FrostedBlue) }
            }
        }
        status?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = if (it.contains("fail", true)) Color(0xFFF87171) else LumenColors.FrostedBlue, fontSize = 13.sp)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun CreatorCard(
    title: String, subtitle: String, enabled: Boolean = true, locked: Boolean = false, onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(LumenColors.DeepGraphite)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled || locked, onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = if (locked) LumenColors.MistGray else LumenColors.FrostWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (locked) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Lock, null, tint = LumenColors.MistGray, modifier = Modifier.size(14.dp))
                }
            }
            Text(subtitle, color = LumenColors.MistGray, fontSize = 12.sp)
        }
    }
}
