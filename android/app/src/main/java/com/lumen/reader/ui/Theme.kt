package com.lumen.reader.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LumenDark = darkColorScheme(
    primary = Color(0xFF7BC6FF),
    onPrimary = Color(0xFF0A0C0F),
    background = Color(0xFF0A0C0F),
    surface = Color(0xFF0F1218),
    onBackground = Color(0xFFF5F7FA),
    onSurface = Color(0xFFF5F7FA)
)

@Composable
fun LumenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LumenDark,
        content = content
    )
}
