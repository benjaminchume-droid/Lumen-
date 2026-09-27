package com.lumen.reader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.reader.data.SupabaseClient
import kotlinx.coroutines.launch

enum class AuthStep { MODE, EMAIL_PASSWORD, EMAIL_OTP, OTP, INTERESTS, DONE }

@Composable
fun AuthScreen(onBack: () -> Unit, onComplete: () -> Unit) {
    BackHandler(onBack = onBack)
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(AuthStep.MODE) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var interest by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var isSignUp by remember { mutableStateOf(true) }

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
        modifier = Modifier
            .fillMaxSize()
            .background(LumenColors.SoftBlack)
            .imePadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        TextButton(onClick = onBack) { Text("← Back", color = LumenColors.FrostedBlue) }
        Text(
            when (step) {
                AuthStep.MODE -> "Account"
                AuthStep.EMAIL_PASSWORD -> if (isSignUp) "Create account" else "Sign in"
                AuthStep.EMAIL_OTP -> "Sign in with email"
                AuthStep.OTP -> "Enter code"
                AuthStep.INTERESTS -> "Your interests"
                AuthStep.DONE -> "You're in"
            },
            color = LumenColors.FrostWhite, fontSize = 26.sp, fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            when (step) {
                AuthStep.MODE -> "Sign in to sync library, or continue with email code."
                AuthStep.EMAIL_PASSWORD -> "Email and password · then pick interests."
                AuthStep.EMAIL_OTP -> "We'll email a 6-digit code."
                AuthStep.OTP -> "Check $email for the code."
                AuthStep.INTERESTS -> "Pick genres you read most (optional)."
                AuthStep.DONE -> "Library sync is ready when the backend is online."
            },
            color = LumenColors.MistGray, fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        when (step) {
            AuthStep.MODE -> {
                Button(
                    onClick = { isSignUp = false; step = AuthStep.EMAIL_PASSWORD },
                    colors = ButtonDefaults.buttonColors(containerColor = LumenColors.FrostedBlue),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text("Sign in with email & password", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold) }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { isSignUp = true; step = AuthStep.EMAIL_PASSWORD },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text("Create account", color = LumenColors.FrostWhite) }
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = { step = AuthStep.EMAIL_OTP }, modifier = Modifier.fillMaxWidth()) {
                    Text("Use 6-digit email code instead", color = LumenColors.FrostedBlue)
                }
            }
            AuthStep.EMAIL_PASSWORD -> {
                OutlinedTextField(value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Email", color = LumenColors.MistGray) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(14.dp), colors = fieldColors)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Password (min 6)", color = LumenColors.MistGray) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(14.dp), colors = fieldColors)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (email.isBlank() || !email.contains("@")) { status = "Enter a valid email"; return@Button }
                        if (password.length < 6) { status = "Password must be at least 6 characters"; return@Button }
                        busy = true; status = null
                        scope.launch {
                            val ok = if (isSignUp) SupabaseClient.signUp(email.trim(), password)
                            else SupabaseClient.signIn(email.trim(), password)
                            busy = false; step = AuthStep.INTERESTS
                            status = if (ok) "Signed in" else "Saved locally — sync when backend is online"
                        }
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = LumenColors.FrostedBlue),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (busy) "Please wait…" else if (isSignUp) "Sign up" else "Sign in",
                        color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold)
                }
            }
            AuthStep.EMAIL_OTP -> {
                OutlinedTextField(value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Email", color = LumenColors.MistGray) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(14.dp), colors = fieldColors)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (email.isBlank() || !email.contains("@")) { status = "Enter a valid email"; return@Button }
                        busy = true; status = null
                        scope.launch {
                            SupabaseClient.sendOtp(email.trim())
                            busy = false; step = AuthStep.OTP; status = "Code requested"
                        }
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = LumenColors.FrostedBlue),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text(if (busy) "Sending…" else "Send code", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold) }
            }
            AuthStep.OTP -> {
                OutlinedTextField(value = otp, onValueChange = { if (it.length <= 6) otp = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("6-digit code", color = LumenColors.MistGray) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp), colors = fieldColors)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (otp.length < 6) { status = "Enter the 6-digit code"; return@Button }
                        busy = true
                        scope.launch {
                            val ok = SupabaseClient.verifyOtp(email.trim(), otp)
                            busy = false; step = AuthStep.INTERESTS
                            status = if (ok) "Verified" else "Saved locally — verify when backend is online"
                        }
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = LumenColors.FrostedBlue),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text(if (busy) "Checking…" else "Verify", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold) }
            }
            AuthStep.INTERESTS -> {
                OutlinedTextField(value = interest, onValueChange = { interest = it }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Fantasy, Manhwa, Sci-Fi", color = LumenColors.MistGray) }, singleLine = true,
                    shape = RoundedCornerShape(14.dp), colors = fieldColors)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch { if (interest.isNotBlank()) SupabaseClient.saveInterests(interest.trim()) }
                        step = AuthStep.DONE; onComplete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LumenColors.FrostedBlue),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text("Finish", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold) }
                TextButton(onClick = { step = AuthStep.DONE; onComplete() }) { Text("Skip", color = LumenColors.MistGray) }
            }
            AuthStep.DONE -> {
                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(containerColor = LumenColors.FrostedBlue),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text("Continue", color = LumenColors.SoftBlack, fontWeight = FontWeight.SemiBold) }
            }
        }
        status?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = LumenColors.MistGray, fontSize = 12.sp)
        }
    }
}
