package com.ghartak.app.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand: deep indigo → violet, with a warm amber accent used sparingly (promos, live status).
val Indigo700 = Color(0xFF3730A3)
val Indigo600 = Color(0xFF4F46E5)
val Indigo500 = Color(0xFF6366F1)
val Indigo100 = Color(0xFFE0E7FF)
val Violet600 = Color(0xFF7C3AED)
val Amber500 = Color(0xFFF59E0B)
val Amber100 = Color(0xFFFEF3C7)
val Orange500 = Color(0xFFFF7A1A)

val SuccessGreen = Color(0xFF16A34A)
val SuccessGreenSoft = Color(0xFFDCFCE7)
val ErrorRed = Color(0xFFDC2626)
val ErrorRedSoft = Color(0xFFFEE2E2)
val InfoBlueSoft = Color(0xFFDBEAFE)

val Ink900 = Color(0xFF111827)
val Ink600 = Color(0xFF6B7280)
val Line200 = Color(0xFFE5E7EB)
val CanvasLight = Color(0xFFF6F7FB)

val CanvasDark = Color(0xFF0F1115)
val SurfaceDark = Color(0xFF171A21)
val SurfaceVariantDark = Color(0xFF212633)
val LineDark = Color(0xFF2B3140)
val InkOnDark = Color(0xFFF3F4F6)
val InkOnDarkMuted = Color(0xFF9CA3AF)

/** Primary brand gradient — headers, primary buttons, logo. */
val BrandGradient: Brush = Brush.linearGradient(listOf(Indigo600, Violet600))

/** Warm gradient for promotional surfaces. */
val PromoGradient: Brush = Brush.linearGradient(listOf(Orange500, Amber500))
