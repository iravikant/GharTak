package com.ghartak.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    primaryContainer = Indigo100,
    onPrimaryContainer = Indigo700,
    secondary = Amber500,
    onSecondary = Color.White,
    secondaryContainer = Amber100,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Violet600,
    background = CanvasLight,
    onBackground = Ink900,
    surface = Color.White,
    onSurface = Ink900,
    surfaceVariant = Color(0xFFF1F2F7),
    onSurfaceVariant = Ink600,
    outline = Color(0xFFD1D5DB),
    outlineVariant = Line200,
    error = ErrorRed,
    errorContainer = ErrorRedSoft,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8B93FF),
    onPrimary = Color(0xFF0B0D3B),
    primaryContainer = Color(0xFF2A2E73),
    onPrimaryContainer = Indigo100,
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF3B2300),
    secondaryContainer = Color(0xFF4A3300),
    onSecondaryContainer = Amber100,
    tertiary = Color(0xFFB794F6),
    background = CanvasDark,
    onBackground = InkOnDark,
    surface = SurfaceDark,
    onSurface = InkOnDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = InkOnDarkMuted,
    outline = Color(0xFF3B4254),
    outlineVariant = LineDark,
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF4C1D1D),
    onErrorContainer = Color(0xFFFECACA)
)

/**
 * Dynamic (wallpaper) colour is deliberately off: a delivery brand needs a consistent identity
 * on every device, and Material You would repaint the whole app in the user's wallpaper colours.
 */
@Composable
fun YeLoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = YeLoTypography,
        shapes = YeLoShapes,
        content = content
    )
}
