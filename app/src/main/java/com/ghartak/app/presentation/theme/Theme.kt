package com.ghartak.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Blue600,
    onPrimary = Color.White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue900,
    secondary = Green700,
    onSecondary = Color.White,
    secondaryContainer = Green100,
    onSecondaryContainer = Green900,
    tertiary = Blue500,
    background = CanvasLight,
    onBackground = Ink900,
    surface = Color.White,
    onSurface = Ink900,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Ink600,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorRed,
    errorContainer = ErrorRedSoft,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FA6FF),
    onPrimary = Color(0xFF04163F),
    primaryContainer = Color(0xFF12306B),
    onPrimaryContainer = Blue100,
    secondary = Green400,
    onSecondary = Color(0xFF03301C),
    secondaryContainer = Color(0xFF0C4A31),
    onSecondaryContainer = Green100,
    tertiary = Blue300,
    background = CanvasDark,
    onBackground = InkOnDark,
    surface = SurfaceDark,
    onSurface = InkOnDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = InkOnDarkMuted,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF4C1D1D),
    onErrorContainer = Color(0xFFFECACA)
)

/**
 * Dynamic (wallpaper) colour is deliberately off: the brand is blue + green on black/white and must look
 * the same on every device.
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
