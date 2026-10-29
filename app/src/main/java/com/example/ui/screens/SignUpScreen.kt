package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderMuted
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onBackToLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun showError(message: String) {
        errorMessage = message
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun normalizePhone(raw: String): String {
        val digitsOnly = raw.replace(Regex("[^0-9]"), "")
        return if (digitsOnly.startsWith("91") && digitsOnly.length > 10) {
            digitsOnly.substring(2)
        } else {
            digitsOnly
        }
    }

    fun handleSignUp() {
        val trimmedName = fullName.trim()
        val trimmedEmail = email.trim()
        val trimmedPhone = phone.trim()
        val trimmedPassword = password.trim()
        val trimmedConfirm = confirmPassword.trim()

        if (trimmedName.isBlank()) {
            showError("Please enter your full name.")
            return
        }
        if (trimmedEmail.isBlank()) {
            showError("Please enter your email.")
            return
        }
        if (trimmedPhone.isBlank()) {
            showError("Please enter your mobile number.")
            return
        }
        if (trimmedPassword.isBlank() || trimmedConfirm.isBlank()) {
            showError("Please enter and confirm your password.")
            return
        }
        if (trimmedPassword.length < 6) {
            showError("Password must be at least 6 characters.")
            return
        }
        if (trimmedPassword != trimmedConfirm) {
            showError("Passwords do not match.")
            return
        }

        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            try {
                val auth = FirebaseAuth.getInstance()
                val safeEmail = trimmedEmail
                val result = auth.createUserWithEmailAndPassword(safeEmail, trimmedPassword).await()
                val user = result.user
                if (user != null) {
                    val cleanedPhone = normalizePhone(trimmedPhone)
                    user.updateProfile(
                        com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(trimmedName)
                            .build()
                    ).await()
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                        onSignUpSuccess()
                        return@launch
                    }
                }
                showError("Sign up completed but user data could not be finalized.")
            } catch (e: Throwable) {
                val rawMessage = e.localizedMessage ?: e.message ?: "Sign up failed."
                val friendlyMessage = when {
                    rawMessage.contains("email-already-in-use", ignoreCase = true) ->
                        "This email is already registered. Please log in instead."
                    rawMessage.contains("invalid-email", ignoreCase = true) ->
                        "Please enter a valid email address."
                    rawMessage.contains("weak-password", ignoreCase = true) ->
                        "Password is too weak. Use at least 6 characters."
                    rawMessage.contains("network", ignoreCase = true) ->
                        "Network error. Please try again."
                    else -> rawMessage
                }
                showError(friendlyMessage)
            } finally {
                isLoading = false
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
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToLogin) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to Login",
                    tint = NeonCyan
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Create account",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it; errorMessage = null },
            label = { Text("Full name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
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
            value = email,
            onValueChange = { email = it; errorMessage = null },
            label = { Text("Email") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
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
            value = phone,
            onValueChange = { phone = it; errorMessage = null },
            label = { Text("Mobile number") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
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
            onValueChange = { password = it; errorMessage = null },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
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

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; errorMessage = null },
            label = { Text("Confirm password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
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
            onClick = { handleSignUp() },
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
                text = if (isLoading) "Creating account..." else "Sign Up",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
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
        }

        Spacer(modifier = Modifier.height(18.dp))

        TextButton(onClick = onBackToLogin) {
            Text(
                text = "Already have an account? Log in",
                color = NeonCyan,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

