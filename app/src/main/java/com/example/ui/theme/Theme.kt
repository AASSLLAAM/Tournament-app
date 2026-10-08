package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsportsDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF001F28),
    primaryContainer = Color(0xFF004D61),
    onPrimaryContainer = Color(0xFFBCE9FF),
    secondary = GoldAccent,
    onSecondary = Color(0xFF261900),
    secondaryContainer = Color(0xFF4D3800),
    onSecondaryContainer = Color(0xFFFFE088),
    tertiary = EmeraldGreen,
    onTertiary = Color(0xFF00391A),
    background = DarkNavy,
    onBackground = TextPrimary,
    surface = CardNavy,
    onSurface = TextPrimary,
    surfaceVariant = CardNavyElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderMuted,
    error = CrimsonRed,
    onError = Color.White
)

@Composable
fun WorldWarTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EsportsDarkColorScheme,
        typography = Typography,
        content = content
    )
}
