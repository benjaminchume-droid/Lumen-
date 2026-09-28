package com.lumen.reader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.reader.data.AuthSession
import com.lumen.reader.data.SupabaseClient
import kotlinx.coroutines.launch

enum class AuthStep {
    MODE, SIGN_UP_CREDS, SIGN_UP_OTP, SIGN_UP_USERNAME, SIGN_UP_INTERESTS,
    SIGN_IN, FORGOT_EMAIL, FORGOT_OTP, FORGOT_NEW_PASS, DONE
}

@Composable
fun AuthScreen(onBack: () -> Unit, onComplete: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(AuthStep.MODE) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var interest by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = LumenColors.FrostedBlue,
        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        cursorColor = LumenColors.FrostedBlue,
        focusedContainerColor = LumenColors.DeepGraphite,
        unfocusedContainerColor = LumenColors.DeepGraphite
    )

    fun title(): String = when (step) {
        AuthStep.MODE -> "Account"
        AuthStep.SIGN_UP_CREDS -> "Create account"
        AuthStep.SIGN_UP_OTP -> "Verify email"
        AuthStep.SIGN_UP_USERNAME -> "Choose username"
        AuthStep.SIGN_UP_INTERESTS -> "Your interests"
        AuthStep.SIGN_IN -> "Sign in"
        AuthStep.FORGOT_EMAIL -> "Forgot password"
        AuthStep.FORGOT_OTP -> "Enter reset code"
        AuthStep.FORGOT_NEW_PASS -> "New password"
        AuthStep.DONE -> "You're in"
    }

    fun subtitle(): String = when (step) {
        AuthStep.MODE -> "Sign in or create an account to publish, comment, and sync."
        AuthStep.SIGN_UP_CREDS -> "Email and password · we will send a verification code."
        AuthStep.SIGN_UP_OTP -> "Enter the 6-digit code sent to $email"
        AuthStep.SIGN_UP_USERNAME -> "Pick a unique username (min 3 characters)."
        AuthStep.SIGN_UP_INTERESTS -> "Genres you like — Fantasy, Manhwa, Romance…"
        AuthStep.SIGN_IN -> "Email and password"
        AuthStep.FORGOT_EMAIL -> "We will email a reset code."
        AuthStep.FORGOT_OTP -> "Code sent to $email"
        AuthStep.FORGOT_NEW_PASS -> "Choose a new password (min 6)."
        AuthStep.DONE -> "Welcome to Lumen."
    }

    Column(
        Modifier.fillMaxSize().background(LumenColors.SoftBlack).imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        TextButton(onClick = {
            when (step) {
                AuthStep.MODE -> onBack()
                AuthStep.SIGN_UP_OTP -> step = AuthStep.SIGN_UP_CREDS
                AuthStep.SIGN_UP_USERNAME -> step = AuthStep.SIGN_UP_OTP
                AuthStep.SIGN_UP_INTERESTS -> step = AuthStep.SIGN_UP_USERNAME
                AuthStep.FORGOT_OTP -> step = AuthStep.FORGOT_EMAIL
                AuthStep.FORGOT_NEW_PASS -> step = AuthStep.FORGOT_OTP
                AuthStep.FORGOT_EMAIL -> step = AuthStep.SIGN_IN
                else -> step = AuthStep.MODE
            }
            status = null
        }) { Text("← Back", color = LumenColors.FrostedBlue) }

        Text(title(), color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle(), color = LumenColors.MistGray, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))

        when (step) {
            AuthStep.MODE -> {
                Button(onClick = { step = AuthStep.SIGN_IN; status = null },
                    colors = ButtonDefaults.buttonColors(containerColor = LumenColors.FrostedBlue),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text("Sign in", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(10.dp))
                Button(onClick = { step = AuthStep.SIGN_UP_CREDS; status = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.12f)),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text("Create account", color = LumenColors.FrostWhite) }
            }
            AuthStep.SIGN_UP_CREDS -> {
                OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(),
                    placeholder = { Text("Email", color = LumenColors.MistGray) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(14.dp), colors = fieldColors)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(),
                    placeholder = { Text("Password (min 6)", color = LumenColors.MistGray) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(14.dp), colors = fieldColors)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    if (email.isBlank() || !email.contains("@")) { status = "Enter a valid email"; return@Button }
                    if (password.length < 6) { status = "Password must be at least 6 characters"; return@Button }
                    busy = true; status = null
                    scope.launch {
                        val signed = SupabaseClient.signUp(email.trim(), password)
                        if (!signed) {
                            busy = false
                            status = SupabaseClient.lastError ?: "Sign up failed. Try a different email or sign in."
                            return@launch
                        }
                        val sent = SupabaseClient.sendOtp(email.trim())
                        busy = false
                        if (sent) {
                            step = AuthStep.SIGN_UP_OTP
                            status = "Verification code sent to your email"
                        } else if (!SupabaseClient.accessToken.isNullOrBlank()) {
                            step = AuthStep.SIGN_UP_USERNAME
                            status = "Account created — choose a username"
                        } else {
                            status = SupabaseClient.lastError ?: "Could not send verification code"
                        }
                    }
                }, enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = LumenColors.FrostedBlue),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text(if (busy) "Please wait…" else "Continue", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold) }
            }
            AuthStep.SIGN_UP_OTP -> {
                OutlinedTextField(otp, { if (it.length <= 6) otp = it.filter { c -> c.isDigit() } },
                    Modifier.fillMaxWidth(),
                    placeholder = { Text("6-digit code", color = LumenColors.MistGray) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp), colors = fieldColors)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    if (otp.length < 6) { status = "Enter the 6-digit code"; return@Button }
                    busy = true; status = null
                    scope.launch {
                        val ok = SupabaseClient.verifyOtp(email.trim(), otp)
                        busy = false
                        if (ok) {
                            AuthSession.markSignedIn(context, email.trim())
                            step = AuthStep.SIGN_UP_USERNAME
                            status = "Email verified"
                        } else {
                            status = SupabaseClient.lastError ?: "Invalid or expired code. Try again."
                        }
                    }
                }, enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = LumenColors.FrostedBlue),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text(if (busy) "Verifying…" else "Verify code", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold) }
                TextButton(onClick = {
                    busy = true
                    scope.launch {
                        SupabaseClient.sendOtp(email.trim())
                        busy = false
                        status = "Code resent"
                    }
                }, enabled = !busy) { Text("Resend code", color = LumenColors.FrostedBlue) }
            }
            AuthStep.SIGN_UP_USERNAME -> {
                OutlinedTextField(username, { username = it.filter { c -> c.isLetterOrDigit() || c == '_' }.take(24) },
                    Modifier.fillMaxWidth(),
                    placeholder = { Text("username", color = LumenColors.MistGray) }, singleLine = true,
                    shape = RoundedCornerShape(14.dp), colors = fieldColors)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    val u = username.trim()
                    if (u.length < 3) { status = "Username must be at least 3 characters"; return@Button }
                    busy = true; status = null
                    scope.launch {
                        if (!SupabaseClient.isUsernameAvailable(u)) {
                            busy = false
                            status = "Username is taken. Try another."
                            return@launch
                        }
                        val saved = SupabaseClient.upsertProfile(u, email.trim())
                        busy = false
                        if (saved) {
                            AuthSession.markSignedIn(context, email.trim(), u)
                            step = AuthStep.SIGN_UP_INTERESTS
                        } else {
                            status = SupabaseClient.lastError ?: "Could not save username"
                        }
                    }
                }, enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = LumenColors.FrostedBlue),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text(if (busy) "Checking…" else "Continue", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold) }
            }
            AuthStep.SIGN_UP_INTERESTS -> {
                OutlinedTextField(interest, { interest = it }, Modifier.fillMaxWidth(),
                    placeholder = { Text("Fantasy, Manhwa, Sci-Fi, Romance…", color = LumenColors.MistGray) },
                    singleLine = true, shape = RoundedCornerShape(14.dp), colors = fieldColors)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    busy = true
                    scope.launch {
                        if (interest.isNotBlank()) {
                            val genres = interest.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                            SupabaseClient.saveInterests(genres)
                        }
                        busy = false
                        step = AuthStep.DONE
                        onComplete()
                    }
                }, enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = LumenColors.FrostedBlue),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text("Finish", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold) }
                TextButton(onClick = { step = AuthStep.DONE; onComplete() }) {
                    Text("Skip for now", color = LumenColors.MistGray)
                }
            }
            AuthStep.SIGN_IN -> {
                OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(),
                    placeholder = { Text("Email", color = LumenColors.MistGray) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(14.dp), colors = fieldColors)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(),
                    placeholder = { Text("Password", color = LumenColors.MistGray) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(14.dp), colors = fieldColors)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { step = AuthStep.FORGOT_EMAIL; status = null }) {
                    Text("Forgot password?", color = LumenColors.FrostedBlue)
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    if (email.isBlank() || !email.contains("@")) { status = "Enter a valid email"; return@Button }
                    if (password.length < 6) { status = "Enter your password"; return@Button }
                    busy = true; status = null
                    scope.launch {
                        val ok = SupabaseClient.signIn(email.trim(), password)
                        busy = false
                        if (ok) {
                            AuthSession.markSignedIn(context, email.trim())
                            step = AuthStep.DONE
                            onComplete()
                        } else {
                            status = SupabaseClient.lastError ?: "Invalid email or password"
                        }
                    }
                }, enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = LumenColors.FrostedBlue),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text(if (busy) "Signing in…" else "Sign in", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold) }
            }
            AuthStep.FORGOT_EMAIL -> {
                OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(),
                    placeholder = { Text("Email", color = LumenColors.MistGray) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(14.dp), colors = fieldColors)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    if (email.isBlank() || !email.contains("@")) { status = "Enter a valid email"; return@Button }
                    busy = true; status = null
                    scope.launch {
                        val ok = SupabaseClient.sendRecoveryOtp(email.trim())
                        busy = false
                        if (ok) { step = AuthStep.FORGOT_OTP; status = "Reset code sent" }
                        else status = SupabaseClient.lastError ?: "Could not send reset code"
                    }
                }, enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = LumenColors.FrostedBlue),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text(if (busy) "Sending…" else "Send reset code", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold) }
            }
            AuthStep.FORGOT_OTP -> {
                OutlinedTextField(otp, { if (it.length <= 6) otp = it.filter { c -> c.isDigit() } },
                    Modifier.fillMaxWidth(),
                    placeholder = { Text("6-digit code", color = LumenColors.MistGray) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp), colors = fieldColors)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    if (otp.length < 6) { status = "Enter the 6-digit code"; return@Button }
                    busy = true; status = null
                    scope.launch {
                        val ok = SupabaseClient.verifyRecoveryOtp(email.trim(), otp)
                        busy = false
                        if (ok) { step = AuthStep.FORGOT_NEW_PASS; status = "Code verified — set a new password" }
                        else status = SupabaseClient.lastError ?: "Invalid or expired code"
                    }
                }, enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = LumenColors.FrostedBlue),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text(if (busy) "Checking…" else "Verify", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold) }
            }
            AuthStep.FORGOT_NEW_PASS -> {
                OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(),
                    placeholder = { Text("New password (min 6)", color = LumenColors.MistGray) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(14.dp), colors = fieldColors)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    if (password.length < 6) { status = "Password must be at least 6 characters"; return@Button }
                    busy = true; status = null
                    scope.launch {
                        val ok = SupabaseClient.updatePassword(password)
                        busy = false
                        if (ok) {
                            AuthSession.markSignedIn(context, email.trim())
                            step = AuthStep.DONE
                            onComplete()
                        } else status = SupabaseClient.lastError ?: "Could not update password"
                    }
                }, enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = LumenColors.FrostedBlue),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text(if (busy) "Saving…" else "Save password", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold) }
            }
            AuthStep.DONE -> {
                Button(onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(containerColor = LumenColors.FrostedBlue),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text("Continue", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold) }
            }
        }
        status?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = if (it.contains("fail", true) || it.contains("Invalid", true) || it.contains("taken", true))
                Color(0xFFF87171) else LumenColors.MistGray, fontSize = 12.sp)
        }
        Spacer(Modifier.height(40.dp))
    }
}
