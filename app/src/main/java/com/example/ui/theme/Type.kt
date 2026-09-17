package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Vira Design System - Premium Typography Hierarchy
 * 
 * Rules:
 * - Single consistent modern sans-serif typeface (System Sans with custom optical tracking).
 * - Numeric values dominate visually with tight negative tracking.
 * - Confident hierarchy from DisplayNumber (58sp) down to Metadata (12sp).
 */
object ViraTypography {
    // 1. Display Hero Number (48-64sp)
    val DisplayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 58.sp,
        lineHeight = 62.sp,
        letterSpacing = (-2.5).sp
    )

    // 2. Secondary Display / Big Values
    val DisplayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.2).sp
    )

    // 3. Screen Title (28-32sp)
    val ScreenTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.6).sp
    )

    // 4. Section Title (18-22sp)
    val SectionTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.3).sp
    )

    // 5. Eyebrow Tag / Structured Category Label
    val Eyebrow = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 1.6.sp
    )

    // 6. Body Primary (15-17sp)
    val Body = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp
    )

    // 7. Body Secondary / Metadata (13-14sp)
    val BodySecondary = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp
    )

    // 8. Action & Button Label (15-16sp)
    val ButtonLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    )

    // 9. Caption / Fine Print (12sp)
    val Caption = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    )

    // Backward-compatible aliases
    val HeroNumber = DisplayLarge
    val HeroLabel = Eyebrow
    val MetricLarge = DisplayMedium
    val MetricMedium = SectionTitle
    val PageTitle = ScreenTitle
}

/**
 * Material 3 Typography Bridge
 */
val Typography = Typography(
    displayLarge = ViraTypography.DisplayLarge,
    headlineLarge = ViraTypography.DisplayMedium,
    headlineMedium = ViraTypography.ScreenTitle,
    titleLarge = ViraTypography.ScreenTitle,
    titleMedium = ViraTypography.SectionTitle,
    titleSmall = ViraTypography.Eyebrow,
    bodyLarge = ViraTypography.Body,
    bodyMedium = ViraTypography.BodySecondary,
    labelLarge = ViraTypography.ButtonLabel,
    labelSmall = ViraTypography.Caption
)
