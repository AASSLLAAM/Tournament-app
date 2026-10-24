package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EsportsRepository
import com.example.ui.theme.BorderCyan
import com.example.ui.theme.BorderMuted
import com.example.ui.theme.CardNavy
import com.example.ui.theme.CardNavyElevated
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class AuthMethod {
    PHONE,
    EMAIL
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Primary Tab Selection: Phone (default) vs Email
    var selectedAuthMethod by remember { mutableStateOf(AuthMethod.PHONE) }

    // Phone Tab States
    var isPhoneSignUpMode by remember { mutableStateOf(false) }
    var phoneUsername by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var phonePassword by remember { mutableStateOf("") }
    var phonePasswordVisible by remember { mutableStateOf(false) }

    // Email Tab States
    var isEmailSignUpMode by remember { mutableStateOf(false) }
    var emailUsername by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var emailPassword by remember { mutableStateOf("") }
    var emailPasswordVisible by remember { mutableStateOf(false) }

    // Common Loading & Error States
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Forgot Password Dialog States
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }
    var isResetLoading by remember { mutableStateOf(false) }
    var resetDialogError by remember { mutableStateOf<String?>(null) }

    // Physical Back button handler: close dialog if open
    BackHandler(enabled = showForgotPasswordDialog) {
        if (!isResetLoading) {
            showForgotPasswordDialog = false
            resetDialogError = null
        }
    }

    // Clean Phone number helper (strips 91 prefix if user pasted full number)
    fun extractClean10DigitPhone(input: String): String {
        val digits = input.filter { it.isDigit() }
        return when {
            digits.startsWith("91") && digits.length == 12 -> digits.substring(2)
            else -> digits
        }
    }

    // Phone + Password Authentication (Sign In & Create Account without OTP)
    fun executePhoneAuth() {
        val cleanPhone = extractClean10DigitPhone(phoneNumber)
        val cleanPassword = phonePassword.trim()
        val cleanUsername = phoneUsername.trim()

        if (cleanPhone.length != 10) {
            val msg = "Invalid phone number. Please enter a valid 10-digit number."
            errorMessage = msg
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            return
        }

        if (cleanPassword.length < 6) {
            val msg = "Password must be at least 6 characters."
            errorMessage = msg
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            return
        }

        if (isPhoneSignUpMode && cleanUsername.isBlank()) {
            val msg = "Please enter your Gamer Tag / Username."
            errorMessage = msg
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            return
        }

        // Identifier created with dummy domain as required: ${phoneNumber}@worldwar.app
        val dummyEmail = "${cleanPhone}@worldwar.app"
        val actualPhoneWithCode = "+91$cleanPhone"

        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            try {
                val auth = try {
                    FirebaseAuth.getInstance()
                } catch (_: Throwable) {
                    isLoading = false
                    val msg = "Firebase service unavailable. Please check internet connection."
                    errorMessage = msg
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    return@launch
                }

                if (isPhoneSignUpMode) {
                    // Create Account with dummy email & password
                    val authResult = auth.createUserWithEmailAndPassword(dummyEmail, cleanPassword).await()
                    val user = authResult.user
                    val uid = user?.uid ?: ""

                    // Update displayName to username
                    try {
                        user?.updateProfile(
                            userProfileChangeRequest {
                                displayName = cleanUsername
                            }
                        )?.await()
                    } catch (_: Throwable) {}

                    // Save actual phoneNumber, username, and email (dummy) to Firestore under users/{uid}
                    if (uid.isNotBlank()) {
                        try {
                            val firestore = EsportsRepository.createFirestoreInstance(context)
                            val userDocRef = firestore.collection("users").document(uid)
                            val profileMap = mapOf(
                                "userId" to uid,
                                "username" to cleanUsername,
                                "phoneNumber" to actualPhoneWithCode,
                                "email" to dummyEmail,
                                "ingameId" to "WW-${(1000..9999).random()}",
                                "role" to "player",
                                "level" to 5,
                                "tier" to "PRO WARRIOR",
                                "matchesPlayed" to 0,
                                "totalWins" to 0,
                                "totalKills" to 0,
                                "winRate" to "0%",
                                "walletBalance" to 500,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                            userDocRef.set(profileMap).await()

                            // Initial ₹500 welcome bonus transaction
                            val txId = "tx_welcome_${System.currentTimeMillis() % 100000}"
                            val txMap = mapOf(
                                "id" to txId,
                                "userId" to uid,
                                "title" to "Sign-up Welcome Bonus",
                                "amount" to 500,
                                "isCredit" to true,
                                "date" to "Just now",
                                "status" to "Success",
                                "timestamp" to FieldValue.serverTimestamp()
                            )
                            userDocRef.collection("transactions").document(txId).set(txMap).await()
                        } catch (_: Throwable) {}
                    }

                    isLoading = false
                    Toast.makeText(context, "Account created! Welcome, $cleanUsername!", Toast.LENGTH_SHORT).show()
                    onAuthSuccess()
                } else {
                    // Sign In with Phone Dummy Email & Password
                    auth.signInWithEmailAndPassword(dummyEmail, cleanPassword).await()
                    isLoading = false
                    Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                    onAuthSuccess()
                }
            } catch (e: Throwable) {
                isLoading = false
                val rawMsg = e.localizedMessage ?: e.message ?: "Authentication failed."
                val friendlyMsg = when {
                    e is com.google.firebase.auth.FirebaseAuthUserCollisionException ||
                    rawMsg.contains("already in use", ignoreCase = true) ||
                    rawMsg.contains("already exists", ignoreCase = true) ||
                    rawMsg.contains("email-already-in-use", ignoreCase = true) ->
                        "Account already exists with this phone number. Please sign in."
                    e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ||
                    rawMsg.contains("wrong-password", ignoreCase = true) ||
                    rawMsg.contains("wrong password", ignoreCase = true) ||
                    rawMsg.contains("invalid-credential", ignoreCase = true) ||
                    rawMsg.contains("invalid password", ignoreCase = true) ->
                        "Wrong password. Please check your password and try again."
                    e is com.google.firebase.auth.FirebaseAuthInvalidUserException ||
                    rawMsg.contains("user-not-found", ignoreCase = true) ||
                    rawMsg.contains("no user record", ignoreCase = true) ->
                        "No account found with this phone number. Please create an account."
                    e is com.google.firebase.auth.FirebaseAuthWeakPasswordException ||
                    rawMsg.contains("weak-password", ignoreCase = true) ->
                        "Password is too weak. Please use at least 6 characters."
                    e is com.google.firebase.FirebaseNetworkException ||
                    rawMsg.contains("network", ignoreCase = true) ->
                        "Network error. Please check your internet connection."
                    else -> rawMsg
                }
                errorMessage = friendlyMsg
                Toast.makeText(context, friendlyMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Email + Password Authentication
    fun executeEmailAuth() {
        val cleanEmail = emailAddress.trim()
        val cleanPassword = emailPassword.trim()
        val cleanUsername = emailUsername.trim()

        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
            errorMessage = "Please enter both email and password."
            return
        }

        if (cleanPassword.length < 6) {
            errorMessage = "Password must be at least 6 characters."
            return
        }

        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            try {
                val auth = try {
                    Firebase.auth
                } catch (_: Throwable) {
                    isLoading = false
                    errorMessage = "Firebase Authentication unavailable. Check your internet connection."
                    return@launch
                }

                if (isEmailSignUpMode) {
                    val authResult = auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword).await()
                    if (cleanUsername.isNotBlank()) {
                        try {
                            authResult.user?.updateProfile(
                                userProfileChangeRequest {
                                    displayName = cleanUsername
                                }
                            )?.await()
                        } catch (_: Throwable) {}
                    }
                } else {
                    auth.signInWithEmailAndPassword(cleanEmail, cleanPassword).await()
                }
                isLoading = false
                onAuthSuccess()
            } catch (e: Throwable) {
                isLoading = false
                val rawMsg = e.localizedMessage ?: e.message ?: "Authentication failed."
                errorMessage = when {
                    e is com.google.firebase.auth.FirebaseAuthUserCollisionException ||
                    rawMsg.contains("already in use", ignoreCase = true) ||
                    rawMsg.contains("already exists", ignoreCase = true) ||
                    rawMsg.contains("email-already-in-use", ignoreCase = true) ->
                        "Account already exists with this email. Tap below to Sign In."
                    e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ||
                    rawMsg.contains("invalid-credential", ignoreCase = true) ||
                    rawMsg.contains("wrong password", ignoreCase = true) ||
                    rawMsg.contains("invalid password", ignoreCase = true) ||
                    rawMsg.contains("invalid-email", ignoreCase = true) ||
                    rawMsg.contains("invalid email", ignoreCase = true) ->
                        "Wrong password or invalid email format."
                    e is com.google.firebase.auth.FirebaseAuthInvalidUserException ||
                    rawMsg.contains("user-not-found", ignoreCase = true) ||
                    rawMsg.contains("no user record", ignoreCase = true) ->
                        "No account found with this email. Tap 'Create Account' above."
                    e is com.google.firebase.auth.FirebaseAuthWeakPasswordException ||
                    rawMsg.contains("weak-password", ignoreCase = true) ->
                        "Password is too weak. Please use at least 6 characters."
                    rawMsg.contains("too-many-requests", ignoreCase = true) ->
                        "Too many attempts. Please try again later."
                    e is com.google.firebase.FirebaseNetworkException ||
                    rawMsg.contains("network", ignoreCase = true) ->
                        "Network error. Please check your internet connection."
                    else -> rawMsg
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkNavy)
            .safeDrawingPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            border = BorderStroke(1.dp, BorderCyan)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Esports Branding Logo
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NeonCyan.copy(alpha = 0.25f), CardNavyElevated)
                            )
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = "Esports Logo",
                        tint = NeonCyan,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "WORLD",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "WAR",
                        color = NeonCyan,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "Esports Tournament Hub",
                    color = GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Requirement 1: Top Tab Selector: 'Phone' (default) and 'Email'
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardNavyElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        // Phone Tab (Default)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (!isLoading) {
                                        selectedAuthMethod = AuthMethod.PHONE
                                        errorMessage = null
                                    }
                                }
                                .testTag("tab_phone_auth"),
                            color = if (selectedAuthMethod == AuthMethod.PHONE) NeonCyan else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 9.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = if (selectedAuthMethod == AuthMethod.PHONE) Color.Black else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Phone",
                                    color = if (selectedAuthMethod == AuthMethod.PHONE) Color.Black else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Email Tab
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (!isLoading) {
                                        selectedAuthMethod = AuthMethod.EMAIL
                                        errorMessage = null
                                    }
                                }
                                .testTag("tab_email_auth"),
                            color = if (selectedAuthMethod == AuthMethod.EMAIL) NeonCyan else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 9.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = if (selectedAuthMethod == AuthMethod.EMAIL) Color.Black else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Email",
                                    color = if (selectedAuthMethod == AuthMethod.EMAIL) Color.Black else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // 1. PHONE TAB: PHONE + PASSWORD AUTHENTICATION
                // ==========================================
                if (selectedAuthMethod == AuthMethod.PHONE) {
                    // Under 'Phone' tab: 'Sign In' vs 'Create Account' sub-toggle
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CardNavyElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(7.dp))
                                    .clickable {
                                        if (!isLoading) {
                                            isPhoneSignUpMode = false
                                            errorMessage = null
                                        }
                                    }
                                    .testTag("phone_tab_sign_in"),
                                color = if (!isPhoneSignUpMode) NeonCyan.copy(alpha = 0.9f) else Color.Transparent,
                                shape = RoundedCornerShape(7.dp)
                            ) {
                                Text(
                                    text = "Sign In",
                                    color = if (!isPhoneSignUpMode) Color.Black else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(7.dp))
                                    .clickable {
                                        if (!isLoading) {
                                            isPhoneSignUpMode = true
                                            errorMessage = null
                                        }
                                    }
                                    .testTag("phone_tab_create_account"),
                                color = if (isPhoneSignUpMode) NeonCyan.copy(alpha = 0.9f) else Color.Transparent,
                                shape = RoundedCornerShape(7.dp)
                            ) {
                                Text(
                                    text = "Create Account",
                                    color = if (isPhoneSignUpMode) Color.Black else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Requirement 2: Create Account fields (Username, Phone Number with +91, Password)
                    if (isPhoneSignUpMode) {
                        OutlinedTextField(
                            value = phoneUsername,
                            onValueChange = {
                                phoneUsername = it
                                errorMessage = null
                            },
                            label = { Text("Gamer Tag / Username") },
                            placeholder = { Text("e.g. Soul_Mortal") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("phone_username_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderMuted,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Phone Number Input with +91 Prefix
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            // Auto-handle if user pasted with 91
                            val clean = if (digits.startsWith("91") && digits.length > 10) digits.substring(2) else digits
                            if (clean.length <= 10) {
                                phoneNumber = clean
                                errorMessage = null
                            }
                        },
                        label = { Text("Mobile Number") },
                        placeholder = { Text("9876543210") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = NeonCyan)
                        },
                        prefix = {
                            Text("+91 ", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phone_number_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderMuted,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password Input
                    OutlinedTextField(
                        value = phonePassword,
                        onValueChange = {
                            phonePassword = it
                            errorMessage = null
                        },
                        label = { Text("Password") },
                        placeholder = { Text("Minimum 6 characters") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan)
                        },
                        trailingIcon = {
                            IconButton(onClick = { phonePasswordVisible = !phonePasswordVisible }) {
                                Icon(
                                    imageVector = if (phonePasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (phonePasswordVisible) "Hide password" else "Show password",
                                    tint = TextMuted
                                )
                            }
                        },
                        visualTransformation = if (phonePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phone_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderMuted,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Phone Auth Submit Button (Sign In or Create Account)
                    Button(
                        onClick = { executePhoneAuth() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("phone_auth_submit_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPhoneSignUpMode) "Creating Account..." else "Signing in...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = if (isPhoneSignUpMode) "Create Account" else "Sign In with Phone",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Forgot Password button on Phone tab
                    if (!isPhoneSignUpMode) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = {
                                resetDialogError = null
                                showForgotPasswordDialog = true
                            },
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("phone_forgot_password_button")
                        ) {
                            Text(
                                text = "Forgot Password?",
                                color = NeonCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // ==========================================
                // 2. EMAIL TAB: EMAIL + PASSWORD AUTHENTICATION
                // ==========================================
                if (selectedAuthMethod == AuthMethod.EMAIL) {
                    // Under 'Email' tab: 'Sign In' vs 'Create Account' sub-toggle
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CardNavyElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(7.dp))
                                    .clickable {
                                        if (!isLoading) {
                                            isEmailSignUpMode = false
                                            errorMessage = null
                                        }
                                    }
                                    .testTag("email_tab_sign_in"),
                                color = if (!isEmailSignUpMode) NeonCyan.copy(alpha = 0.9f) else Color.Transparent,
                                shape = RoundedCornerShape(7.dp)
                            ) {
                                Text(
                                    text = "Sign In",
                                    color = if (!isEmailSignUpMode) Color.Black else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(7.dp))
                                    .clickable {
                                        if (!isLoading) {
                                            isEmailSignUpMode = true
                                            errorMessage = null
                                        }
                                    }
                                    .testTag("email_tab_create_account"),
                                color = if (isEmailSignUpMode) NeonCyan.copy(alpha = 0.9f) else Color.Transparent,
                                shape = RoundedCornerShape(7.dp)
                            ) {
                                Text(
                                    text = "Create Account",
                                    color = if (isEmailSignUpMode) Color.Black else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isEmailSignUpMode) {
                        OutlinedTextField(
                            value = emailUsername,
                            onValueChange = {
                                emailUsername = it
                                errorMessage = null
                            },
                            label = { Text("Gamer Tag / Username") },
                            placeholder = { Text("e.g. Soul_Mortal") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_username_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderMuted,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Email Address field
                    OutlinedTextField(
                        value = emailAddress,
                        onValueChange = {
                            emailAddress = it
                            errorMessage = null
                        },
                        label = { Text("Email Address") },
                        placeholder = { Text("name@example.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = NeonCyan)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderMuted,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Email Password field
                    OutlinedTextField(
                        value = emailPassword,
                        onValueChange = {
                            emailPassword = it
                            errorMessage = null
                        },
                        label = { Text("Password") },
                        placeholder = { Text("Minimum 6 characters") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan)
                        },
                        trailingIcon = {
                            IconButton(onClick = { emailPasswordVisible = !emailPasswordVisible }) {
                                Icon(
                                    imageVector = if (emailPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (emailPasswordVisible) "Hide password" else "Show password",
                                    tint = TextMuted
                                )
                            }
                        },
                        visualTransformation = if (emailPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderMuted,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Submit Button
                    Button(
                        onClick = { executeEmailAuth() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("email_auth_submit_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEmailSignUpMode) "Creating Account..." else "Signing in...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = if (isEmailSignUpMode) "Create Account" else "Sign In with Email",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Forgot Password button on Email tab
                    if (!isEmailSignUpMode) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = {
                                if (emailAddress.isNotBlank()) {
                                    resetEmailInput = emailAddress.trim()
                                }
                                resetDialogError = null
                                showForgotPasswordDialog = true
                            },
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("forgot_password_button")
                        ) {
                            Text(
                                text = "Forgot Password?",
                                color = NeonCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Error Message Display
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFFF5252),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "By signing in, you agree to WorldWar's Tournament Rules & Fair Play Policy.",
                    color = TextMuted.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )

                // Extra bottom clearance so soft keyboard doesn't occlude action buttons
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Requirement 1 & 2 & 3: Forgot Password Dialog (prompts for Gmail/Email and sends reset link)
        if (showForgotPasswordDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isResetLoading) {
                        showForgotPasswordDialog = false
                        resetDialogError = null
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Reset Password",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Enter your Gmail (Email Address) to receive a secure password reset link.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        OutlinedTextField(
                            value = resetEmailInput,
                            onValueChange = {
                                resetEmailInput = it
                                resetDialogError = null
                            },
                            label = { Text("Gmail / Email Address") },
                            placeholder = { Text("e.g. player@gmail.com") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = NeonCyan)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_email_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderMuted,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        if (!resetDialogError.isNullOrBlank()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFFFF5252).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = resetDialogError ?: "",
                                    color = Color(0xFFFF5252),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cleanEmail = resetEmailInput.trim()
                            if (cleanEmail.isBlank()) {
                                val msg = "Please enter your email"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                resetDialogError = msg
                                return@Button
                            }

                            isResetLoading = true
                            resetDialogError = null

                            coroutineScope.launch {
                                try {
                                    FirebaseAuth.getInstance().sendPasswordResetEmail(cleanEmail).await()
                                    isResetLoading = false
                                    Toast.makeText(context, "Password reset link sent to your Gmail!", Toast.LENGTH_LONG).show()
                                    showForgotPasswordDialog = false
                                    resetEmailInput = ""
                                } catch (e: Throwable) {
                                    isResetLoading = false
                                    val rawMsg = e.localizedMessage ?: e.message ?: "Failed to send reset link."
                                    val friendlyMsg = when {
                                        e is com.google.firebase.auth.FirebaseAuthInvalidUserException ||
                                        rawMsg.contains("user-not-found", ignoreCase = true) ||
                                        rawMsg.contains("no user record", ignoreCase = true) ->
                                            "User not found with this email"
                                        e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ||
                                        rawMsg.contains("invalid-email", ignoreCase = true) ->
                                            "Invalid email format. Please enter a valid Gmail address."
                                        e is com.google.firebase.FirebaseNetworkException ||
                                        rawMsg.contains("network", ignoreCase = true) ->
                                            "Network error. Please check your internet connection."
                                        else -> rawMsg
                                    }
                                    resetDialogError = friendlyMsg
                                    Toast.makeText(context, friendlyMsg, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isResetLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("send_reset_link_button")
                    ) {
                        if (isResetLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sending...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Text("Send Reset Link", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            if (!isResetLoading) {
                                showForgotPasswordDialog = false
                                resetDialogError = null
                            }
                        },
                        enabled = !isResetLoading,
                        modifier = Modifier.testTag("cancel_reset_dialog_button")
                    ) {
                        Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                },
                containerColor = CardNavy,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

fun signOutUser(
    context: Context,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    try {
        Firebase.auth.signOut()
    } catch (_: Throwable) {}
    onSignOutComplete()
}
