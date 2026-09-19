package com.ansar.rotitrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF9B4B14),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC7),
    onPrimaryContainer = Color(0xFF351000),
    secondary = Color(0xFF75584A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBC7),
    onSecondaryContainer = Color(0xFF2B160B),
    tertiary = Color(0xFF4C6642),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCEECBE),
    onTertiaryContainer = Color(0xFF0A2105),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF221A15),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF221A15),
    surfaceVariant = Color(0xFFF4DED4),
    onSurfaceVariant = Color(0xFF52443D),
    outline = Color(0xFF85736B),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB68C),
    onPrimary = Color(0xFF572000),
    primaryContainer = Color(0xFF7B3000),
    onPrimaryContainer = Color(0xFFFFDBC7),
    secondary = Color(0xFFE5BFAD),
    onSecondary = Color(0xFF432B1F),
    secondaryContainer = Color(0xFF5C4134),
    onSecondaryContainer = Color(0xFFFFDBC7),
    tertiary = Color(0xFFB2D0A4),
    onTertiary = Color(0xFF1F3718),
    tertiaryContainer = Color(0xFF354E2C),
    onTertiaryContainer = Color(0xFFCEECBE),
    background = Color(0xFF1A120D),
    onBackground = Color(0xFFF1DFD7),
    surface = Color(0xFF1A120D),
    onSurface = Color(0xFFF1DFD7),
    surfaceVariant = Color(0xFF52443D),
    onSurfaceVariant = Color(0xFFD7C2B8),
    outline = Color(0xFFA08D84),
    error = Color(0xFFFFB4AB)
)

@Composable
fun RotiTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
