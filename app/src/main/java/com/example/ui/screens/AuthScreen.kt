package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BorderMuted
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showForgotDialog by remember { mutableStateOf(false) }
    var recoveryEmail by remember { mutableStateOf("") }
    var recoveryEmailError by remember { mutableStateOf<String?>(null) }
    var recoveryLoading by remember { mutableStateOf(false) }

    fun showError(message: String) {
        errorMessage = message
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun normalizePhoneCandidate(raw: String): String {
        val digitsOnly = raw.replace(Regex("[^0-9]"), "")
        return if (digitsOnly.startsWith("91") && digitsOnly.length > 10) {
            digitsOnly.substring(2)
        } else {
            digitsOnly
        }
    }

    fun handleLogin() {
        val trimmedIdentifier = identifier.trim()
        val trimmedPassword = password.trim()

        if (trimmedIdentifier.isBlank()) {
            showError("Please enter your mobile number or email.")
            return
        }

        if (trimmedPassword.isBlank()) {
            showError("Please enter your password.")
            return
        }

        if (trimmedPassword.length < 6) {
            showError("Password must be at least 6 characters.")
            return
        }

        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            try {
                val auth = FirebaseAuth.getInstance()

                val loginEmail = if (trimmedIdentifier.contains("@")) {
                    trimmedIdentifier
                } else {
                    val phoneCandidate = normalizePhoneCandidate(trimmedIdentifier)
                    "${phoneCandidate}@worldwar.app"
                }

                auth.signInWithEmailAndPassword(loginEmail, trimmedPassword).await()

                isLoading = false
                Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                onAuthSuccess()
            } catch (e: Throwable) {
                isLoading = false

                val rawMessage = e.localizedMessage ?: e.message ?: "Authentication failed."
                val friendlyMessage = when {
                    rawMessage.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                        rawMessage.contains("wrong-password", ignoreCase = true) ||
                        rawMessage.contains("wrong password", ignoreCase = true) ||
                        rawMessage.contains("invalid-credential", ignoreCase = true) ||
                        rawMessage.contains("invalid credential", ignoreCase = true) ->
                        "Incorrect email/phone or password. Please try again."

                    rawMessage.contains("user-not-found", ignoreCase = true) ||
                        rawMessage.contains("no user record", ignoreCase = true) ||
                        rawMessage.contains("account-not-found", ignoreCase = true) ->
                        "No account found with this login. Please create a new account."

                    rawMessage.contains("invalid-email", ignoreCase = true) ||
                        rawMessage.contains("invalid email", ignoreCase = true) ||
                        rawMessage.contains("badly formatted", ignoreCase = true) ->
                        "Please enter a valid email address or mobile number."

                    rawMessage.contains("network", ignoreCase = true) ->
                        "Network error. Please check your internet connection."

                    else -> rawMessage
                }

                showError(friendlyMessage)
            }
        }
    }

    fun handlePasswordReset() {
        val email = recoveryEmail.trim()

        if (email.isBlank()) {
            recoveryEmailError = "Please enter your registered Gmail."
            Toast.makeText(context, "Please enter your registered Gmail.", Toast.LENGTH_SHORT).show()
            return
        }

        recoveryLoading = true
        recoveryEmailError = null

        coroutineScope.launch {
            try {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
                Toast.makeText(context, "Password reset link sent to $email", Toast.LENGTH_LONG).show()
                showForgotDialog = false
                recoveryEmail = ""
            } catch (e: Throwable) {
                val rawMessage = e.localizedMessage ?: e.message ?: "Failed to send reset email."
                val friendlyMessage = when {
                    rawMessage.contains("user-not-found", ignoreCase = true) ||
                        rawMessage.contains("no user record", ignoreCase = true) ->
                        "No account is registered with this email."

                    rawMessage.contains("invalid-email", ignoreCase = true) ||
                        rawMessage.contains("invalid email", ignoreCase = true) ->
                        "Please enter a valid Gmail address."

                    rawMessage.contains("network", ignoreCase = true) ->
                        "Network error. Please check your internet connection."

                    else -> rawMessage
                }

                recoveryEmailError = friendlyMessage
                Toast.makeText(context, friendlyMessage, Toast.LENGTH_LONG).show()
            } finally {
                recoveryLoading = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkNavy)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        Image(
            painter = painterResource(id = R.drawable.worldwar_icon),
            contentDescription = "WorldWar logo",
            modifier = Modifier.size(88.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "WORLDWAR",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(42.dp))

        OutlinedTextField(
            value = identifier,
            onValueChange = {
                identifier = it
                errorMessage = null
            },
            placeholder = {
                Text(
                    text = "Mobile number or email",
                    color = TextMuted
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = BorderMuted,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = NeonCyan,
                errorBorderColor = Color(0xFFFF5C5C)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            placeholder = {
                Text(
                    text = "Password",
                    color = TextMuted
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Show password" else "Hide password",
                        tint = TextMuted
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = BorderMuted,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = NeonCyan,
                errorBorderColor = Color(0xFFFF5C5C)
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = { handleLogin() },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan,
                contentColor = Color.Black,
                disabledContainerColor = NeonCyan.copy(alpha = 0.6f),
                disabledContentColor = Color.Black.copy(alpha = 0.7f)
            )
        ) {
            Text(
                text = if (isLoading) "Logging in..." else "Log in",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = {
                recoveryEmail = if (identifier.contains("@")) identifier.trim() else ""
                showForgotDialog = true
                recoveryEmailError = null
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Forgot password?",
                color = NeonCyan,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onNavigateToSignUp,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.8f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = NeonCyan
            )
        ) {
            Text(
                text = "Create a new account",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = Color(0xFFFF5C5C),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = "By signing in, you agree to WorldWAR Terms & Fair Play Policy",
            color = TextMuted.copy(alpha = 0.8f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.padding(bottom = 20.dp)
        )
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!recoveryLoading) {
                    showForgotDialog = false
                    recoveryEmailError = null
                }
            },
            title = {
                Text(
                    text = "Reset password",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your registered Gmail to receive a password reset link.",
                        color = TextMuted,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = recoveryEmail,
                        onValueChange = {
                            recoveryEmail = it
                            recoveryEmailError = null
                        },
                        placeholder = {
                            Text(
                                text = "name@gmail.com",
                                color = TextMuted
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderMuted,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonCyan,
                            errorBorderColor = Color(0xFFFF5C5C)
                        )
                    )

                    if (recoveryEmailError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = recoveryEmailError ?: "",
                            color = Color(0xFFFF5C5C),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { handlePasswordReset() },
                    enabled = !recoveryLoading
                ) {
                    Text(
                        text = if (recoveryLoading) "Sending..." else "Send link",
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!recoveryLoading) {
                            showForgotDialog = false
                            recoveryEmailError = null
                        }
                    }
                ) {
                    Text(
                        text = "Cancel",
                        color = TextMuted
                    )
                }
            },
            containerColor = DarkNavy,
            tonalElevation = 0.dp
        )
    }
}
