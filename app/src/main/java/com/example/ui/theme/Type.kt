package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Vira Design System — Plus Jakarta Sans Typeface Family
 */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans, weight = FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, weight = FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, weight = FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, weight = FontWeight.Bold)
)

/**
 * Vira Design System - Premium Editorial Typography Hierarchy
 * 
 * Rules:
 * - Single consistent modern sans-serif typeface (Plus Jakarta Sans).
 * - Numeric values dominate visually with tight negative tracking.
 * - Confident hierarchy from DisplayLarge (52sp) down to Caption (12sp).
 */
object ViraTypography {
    // 1. Display Hero Number (48-56sp)
    val DisplayLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1.8).sp
    )

    // 2. Secondary Display / Big Values (32-36sp)
    val DisplayMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.8).sp
    )

    // 3. Screen Title (24-28sp)
    val ScreenTitle = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp
    )

    // 4. Section Title (18-20sp)
    val SectionTitle = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    )

    // 5. Eyebrow Tag / Structured Category Label
    val Eyebrow = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.2.sp
    )

    // 6. Body Primary (15-16sp)
    val Body = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )

    // 7. Body Secondary / Metadata (13-14sp)
    val BodySecondary = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    )

    // 8. Action & Button Label (14-15sp)
    val ButtonLabel = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )

    // 9. Caption / Fine Print (12sp)
    val Caption = TextStyle(
        fontFamily = PlusJakartaSans,
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
