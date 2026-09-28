package com.lumen.reader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.reader.data.AuthSession

enum class CreatorView { Hub, CreatePick, NovelForm, ComicForm }

@Composable
fun CreatorStudioScreen(onBack: () -> Unit, onNeedAuth: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val signedIn = AuthSession.isSignedIn(context)
    var view by remember { mutableStateOf(CreatorView.Hub) }
    val scroll = rememberScrollState()

    Column(
        Modifier.fillMaxSize().background(LumenColors.SoftBlack).verticalScroll(scroll).padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(40.dp))
        TextButton(onClick = {
            if (view != CreatorView.Hub) view = CreatorView.Hub else onBack()
        }) { Text("\u2190", color = LumenColors.FrostedBlue, fontSize = 20.sp) }

        when (view) {
            CreatorView.Hub -> {
                Text("Creator Studio", color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Text("Publish novels and comics on Lumen", color = LumenColors.MistGray, fontSize = 13.sp)
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
                            Text("Publishing and analytics need a Lumen account.", color = LumenColors.MistGray, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                listOf(
                    "My Works" to "Series you've published",
                    "Drafts" to "Unpublished chapters",
                    "Published" to "Live on Lumen",
                    "Analytics" to "Reads · reactions",
                    "Comments / Reviews" to "Reader feedback"
                ).forEach { (t, s) ->
                    StudioRow(t, s) { if (!signedIn) onNeedAuth() }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Create", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(LumenColors.FrostedBlue)
                        .clickable { if (!signedIn) onNeedAuth() else view = CreatorView.CreatePick }
                        .padding(vertical = 14.dp).wrapContentWidth().padding(horizontal = 24.dp)
                )
            }
            CreatorView.CreatePick -> {
                Text("What are you creating?", color = LumenColors.FrostWhite, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(20.dp))
                CreateCard("Novel", "Long-form chapters · infinite scroll reader") { view = CreatorView.NovelForm }
                Spacer(Modifier.height(12.dp))
                CreateCard("Comic / Manga", "Page images · swipe reader") { view = CreatorView.ComicForm }
            }
            CreatorView.NovelForm, CreatorView.ComicForm -> {
                val kind = if (view == CreatorView.NovelForm) "Novel" else "Comic"
                Text("New $kind", color = LumenColors.FrostWhite, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Text("Title, cover, and first chapter upload will sync to Supabase when online.", color = LumenColors.MistGray, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                StudioRow("Title & synopsis", "Required") {}
                StudioRow("Cover image", "Optional") {}
                StudioRow("First chapter", "Text or pages") {}
                Spacer(Modifier.height(16.dp))
                Text(
                    "Save draft", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(LumenColors.FrostedBlue)
                        .clickable { view = CreatorView.Hub }.padding(horizontal = 18.dp, vertical = 12.dp)
                )
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun StudioRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(14.dp))
            .background(LumenColors.DeepGraphite).clickable(onClick = onClick).padding(14.dp)
    ) {
        Text(title, color = LumenColors.FrostWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(subtitle, color = LumenColors.MistGray, fontSize = 12.sp)
    }
}

@Composable
private fun CreateCard(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0x337BC6FF), LumenColors.DeepGraphite)))
            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(20.dp)
    ) {
        Text(title, color = LumenColors.FrostWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = LumenColors.MistGray, fontSize = 13.sp)
    }
}
