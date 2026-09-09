package com.lumen.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.reader.ui.LumenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LumenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0C0F)
                ) {
                    HomeScreen()
                }
            }
        }
    }
}

@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C0F))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "LUMEN",
                color = Color(0xFF7BC6FF),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp
            )
            Spacer(Modifier = Modifier.height(8.dp))
            Text(
                text = "Reading OS · Extensions · Aggregator",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp
            )
            Spacer(Modifier = Modifier.height(32.dp))
            Text(
                text = "Extension system + universal chapter aggregator ready.\nInstall sources from Source Manager.",
                color = Color(0xFF64748B),
                fontSize = 12.sp
            )
        }
    }
}
