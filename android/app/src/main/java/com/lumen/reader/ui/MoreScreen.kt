package com.lumen.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.reader.BuildConfig
import com.lumen.reader.data.AuthSession

@Composable
fun MoreScreen(
    onSignIn: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCreator: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val context = LocalContext.current
    val signedIn = AuthSession.isSignedIn(context)
    val username = AuthSession.username(context)
    val email = AuthSession.email(context)
    val displayName = when {
        username.isNotBlank() -> "@$username"
        email.isNotBlank() -> email.substringBefore("@")
        else -> "Guest"
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(LumenColors.SoftBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        Text("More", color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text("Lumen v${BuildConfig.VERSION_NAME}", color = LumenColors.MistGray, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        GlassCard {
            if (signedIn) {
                Text("Signed in", color = LumenColors.MistGray, fontSize = 12.sp)
                Text(displayName, color = LumenColors.FrostWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                if (username.isNotBlank() && email.isNotBlank()) {
                    Text(email, color = LumenColors.MistGray.copy(0.7f), fontSize = 12.sp)
                }
            } else {
                Text("Not signed in", color = LumenColors.MistGray, fontSize = 12.sp)
                Text("Sign up to unlock premium features", color = LumenColors.FrostWhite, fontWeight = FontWeight.SemiBold)
                Text("Publishing · commenting · cloud library · reviews", color = LumenColors.MistGray, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Sign in / Sign up",
                    color = LumenColors.SoftBlack,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(LumenColors.FrostedBlue)
                        .clickable(onClick = onSignIn)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Section("You")
        MoreRow(Icons.Default.Person, "Profile", if (signedIn) displayName else "Sign in required") {
            if (signedIn) onOpenProfile() else onSignIn()
        }
        MoreRow(Icons.Default.Create, "Creator Studio", "My works · drafts · publish novel or comic", onOpenCreator)
        MoreRow(Icons.Default.Download, "Downloads", "Encrypted offline chapters", onOpenDownloads)

        Spacer(Modifier.height(12.dp))
        Section("App")
        MoreRow(Icons.Default.Source, "Sources & extensions", "Keiyoushi + LNReader", onOpenSources)
        MoreRow(Icons.Default.Notifications, "Notifications", "Chapter alerts") { }
        MoreRow(Icons.Default.Settings, "Settings", "Theme · updates · storage", onOpenSettings)
        MoreRow(Icons.Default.Info, "About Lumen", "Version ${BuildConfig.VERSION_NAME}") { }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x337BC6FF), Color(0x22151A22), Color(0xFF12161E))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(18.dp),
        content = content
    )
}

@Composable
private fun Section(title: String) {
    Text(
        title.uppercase(),
        color = LumenColors.MistGray.copy(0.55f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun MoreRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(LumenColors.DeepGraphite)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = LumenColors.FrostedBlue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = LumenColors.FrostWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = LumenColors.MistGray, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = LumenColors.MistGray.copy(0.5f), modifier = Modifier.size(18.dp))
    }
}
