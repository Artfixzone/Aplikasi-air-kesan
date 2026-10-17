package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = KesanGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = KesanGreenContainer,
    onPrimaryContainer = KesanOnGreenContainer,
    secondary = KesanWaterCyan,
    onSecondary = Color.White,
    secondaryContainer = KesanWaterCyanContainer,
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = KesanGreenLight,
    onTertiary = Color(0xFF064E3B),
    error = KesanDebtRed,
    onError = Color.White,
    errorContainer = KesanDebtRedContainer,
    onErrorContainer = KesanOnDebtRedContainer,
    background = KesanBackground,
    onBackground = KesanTextDark,
    surface = KesanSurface,
    onSurface = KesanTextDark,
    surfaceVariant = KesanSurfaceVariant,
    onSurfaceVariant = KesanTextMuted,
    outline = KesanOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = KesanGreenLight,
    onPrimary = Color(0xFF064E3B),
    primaryContainer = KesanGreenDark,
    onPrimaryContainer = KesanGreenContainer,
    secondary = KesanWaterCyan,
    onSecondary = Color.White,
    error = Color(0xFFF87171),
    onError = Color.Black,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF061A14),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF0B241D),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF13362B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF059669)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
