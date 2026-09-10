package com.lumen.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.reader.core.DownloadQueue
import com.lumen.reader.core.DownloadStatus
import com.lumen.reader.core.DownloadTask

@Composable
fun DownloadsScreen(queue: DownloadQueue) {
    val tasks by queue.tasks.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C0F))
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text("Downloads", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Pause · Resume · Progress · Speed",
            color = Color(0xFF64748B),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (tasks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF12151C))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No active downloads", color = Color.White, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Install a source, open a series, then download chapters.",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        queue.enqueue(
                            seriesTitle = "Sample Series",
                            chapterName = "Chapter 1",
                            chapterUrl = "https://example.com/ch1",
                            sourceId = "demo",
                            totalPages = 12
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7BC6FF))
                ) {
                    Text("Test download queue", color = Color(0xFF0A0C0F))
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(tasks, key = { it.id }) { task ->
                    DownloadCard(task = task, queue = queue)
                }
            }
        }
    }
}

@Composable
private fun DownloadCard(task: DownloadTask, queue: DownloadQueue) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF12151C))
            .padding(14.dp)
    ) {
        Text(task.seriesTitle, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(task.chapterName, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = task.progressPercent / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF7BC6FF),
            trackColor = Color(0x22FFFFFF)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${task.progressPercent}% · ${task.pagesDone}/${task.totalPages} · ${formatSpeed(task.speedBps)} · ${task.status.name}",
                color = Color(0xFF64748B),
                fontSize = 11.sp
            )
            Row {
                when (task.status) {
                    DownloadStatus.RUNNING, DownloadStatus.QUEUED -> {
                        TextButton(onClick = { queue.pause(task.id) }) {
                            Text("Pause", color = Color(0xFF7BC6FF), fontSize = 12.sp)
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        TextButton(onClick = { queue.resume(task.id) }) {
                            Text("Resume", color = Color(0xFF34D399), fontSize = 12.sp)
                        }
                    }
                    else -> {}
                }
                if (task.status != DownloadStatus.COMPLETED) {
                    TextButton(onClick = { queue.cancel(task.id) }) {
                        Text("Cancel", color = Color(0xFFF87171), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun formatSpeed(bps: Long): String {
    if (bps <= 0) return "—"
    val kb = bps / 1024.0
    return if (kb >= 1024) String.format("%.1f MB/s", kb / 1024) else String.format("%.0f KB/s", kb)
}
