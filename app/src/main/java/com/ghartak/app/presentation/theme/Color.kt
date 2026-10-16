package com.ghartak.app.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------------------------
// Ye Lo !! palette: blue + green on black and white, blended into gradients.
// ---------------------------------------------------------------------------------------------

// Black
val Black900 = Color(0xFF050A12)
val Black800 = Color(0xFF0B121C)

// Blue
val Blue900 = Color(0xFF0A2A6B)
val Blue700 = Color(0xFF0A47B8)
val Blue600 = Color(0xFF1560E8)
val Blue500 = Color(0xFF3B82F6)
val Blue300 = Color(0xFF9CC4FF)
val Blue100 = Color(0xFFDBE8FF)

// Green
val Green900 = Color(0xFF04472A)
val Green700 = Color(0xFF0B8F58)
val Green600 = Color(0xFF12A566)
val Green400 = Color(0xFF3DD598)
val Green100 = Color(0xFFD3F5E5)

// Semantic
val Amber500 = Color(0xFFF5A524) // ratings and "waiting" states only
val SuccessGreen = Green600
val ErrorRed = Color(0xFFDC2626)
val ErrorRedSoft = Color(0xFFFEE2E2)

// Neutrals (light)
val CanvasLight = Color(0xFFF2F6FB)
val SurfaceVariantLight = Color(0xFFE7EEF7)
val OutlineLight = Color(0xFFC9D4E3)
val OutlineVariantLight = Color(0xFFDEE6F0)
val Ink900 = Color(0xFF0B1220)
val Ink600 = Color(0xFF566377)

// Neutrals (dark)
val CanvasDark = Color(0xFF060B12)
val SurfaceDark = Color(0xFF0D141D)
val SurfaceVariantDark = Color(0xFF16212F)
val OutlineDark = Color(0xFF2A3A4E)
val OutlineVariantDark = Color(0xFF1D2A3A)
val InkOnDark = Color(0xFFEAF0F8)
val InkOnDarkMuted = Color(0xFF93A1B5)

/** Headers and hero surfaces: black → blue → green, top-left to bottom-right. */
val BrandGradient: Brush = Brush.linearGradient(listOf(Black900, Blue700, Green700))

/** Primary buttons: a brighter blue → green sweep. */
val ButtonGradient: Brush = Brush.horizontalGradient(listOf(Blue600, Green700))

/** Promotional surfaces: black into green. */
val PromoGradient: Brush = Brush.linearGradient(listOf(Black800, Green700))
