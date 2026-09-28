package com.lumen.reader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.reader.core.SecureDownloadStore

@Composable
fun DownloadsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val store = remember { SecureDownloadStore(context) }
    val chapters = remember { store.listDownloaded() }

    Column(
        Modifier.fillMaxSize().background(LumenColors.SoftBlack).padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(40.dp))
        TextButton(onClick = onBack) { Text("← Back", color = LumenColors.FrostedBlue) }
        Text("Downloads", color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text("Encrypted offline chapters", color = LumenColors.MistGray, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        if (chapters.isEmpty()) {
            Text("No offline chapters yet. Download from a series detail screen.", color = LumenColors.MistGray, fontSize = 13.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(chapters) { (title, id) ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(LumenColors.DeepGraphite).padding(14.dp)
                    ) {
                        Text(title.ifBlank { id }, color = LumenColors.FrostWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(id, color = LumenColors.MistGray, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
