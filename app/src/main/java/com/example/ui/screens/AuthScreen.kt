package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
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
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthScreen"

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun handleGoogleSignIn() {
        val activity = context as? Activity
        if (activity == null) {
            errorMessage = "Context is not an Activity."
            return
        }

        val serverClientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            errorMessage = "Web client ID not configured. Please ensure Firebase is provisioned."
            return
        }

        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
            serverClientId = serverClientId
        ).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        val credentialManager = CredentialManager.create(context)

        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = activity
                )
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                    Firebase.auth.signInWithCredential(authCredential).await()
                    isLoading = false
                    onAuthSuccess()
                } else {
                    isLoading = false
                    errorMessage = "Unexpected credential format returned from Google."
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Sign-in cancelled by user: ${e.message}")
                isLoading = false
            } catch (e: GetCredentialException) {
                Log.e(TAG, "CredentialManager error", e)
                isLoading = false
                errorMessage = e.localizedMessage ?: "Google Sign-In failed."
            } catch (e: Exception) {
                Log.e(TAG, "Authentication error", e)
                isLoading = false
                errorMessage = e.localizedMessage ?: "Authentication failed. Please try again."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
            .padding(16.dp),
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
                // Logo Icon
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NeonCyan.copy(alpha = 0.25f), CardNavyElevated)
                            )
                        )
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = "Esports Logo",
                        tint = NeonCyan,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "WORLD",
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "WAR",
                        color = NeonCyan,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "Esports Tournament Hub",
                    color = GoldAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Compete in daily Free Fire MAX & BGMI scrims, win real cash prizes, and build your competitive gaming stats.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Highlights
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardNavyElevated)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureHighlightRow(
                        icon = Icons.Default.EmojiEvents,
                        iconTint = GoldAccent,
                        title = "Real Esports Contests",
                        desc = "Solo, Duo & Squad tournaments with verified live rooms"
                    )
                    HorizontalDivider(color = BorderMuted.copy(alpha = 0.5f))
                    FeatureHighlightRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = NeonCyan,
                        title = "₹500 Welcome Bonus",
                        desc = "Instant credits in your wallet to enter your first battle"
                    )
                    HorizontalDivider(color = BorderMuted.copy(alpha = 0.5f))
                    FeatureHighlightRow(
                        icon = Icons.Default.Security,
                        iconTint = Color(0xFF00E676),
                        title = "Verified & Fair Play",
                        desc = "Protected with secure zero-trust cloud sync & anti-cheat"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Google Sign In Button
                Button(
                    onClick = { handleGoogleSignIn() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color.Black
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Connecting to Google...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sign in with Google",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
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
            }
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = desc,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

fun signOutUser(
    context: Context,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    onSignOutComplete()
}
