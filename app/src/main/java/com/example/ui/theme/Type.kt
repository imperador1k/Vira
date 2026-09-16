package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Vira Design System - Typography Tokens
 * 
 * Rules:
 * - Numerical values dominate visually (HeroNumber, MetricLarge, MetricMedium).
 * - Section headers are crisp and structured (SectionTitle).
 * - European minimal aesthetic with tight letter spacing for big numbers.
 */
object ViraTypography {
    val HeroNumber = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 68.sp,
        lineHeight = 72.sp,
        letterSpacing = (-3).sp
    )

    val HeroLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp
    )

    val MetricLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-1).sp
    )

    val MetricMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.5).sp
    )

    val PageTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp
    )

    val SectionTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.2.sp
    )

    val Body = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )

    val BodySecondary = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    )

    val ButtonLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    )

    val Caption = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp
    )
}

/**
 * Standard Material 3 Typography bridge
 */
val Typography = Typography(
    displayLarge = ViraTypography.HeroNumber,
    headlineLarge = ViraTypography.MetricLarge,
    headlineMedium = ViraTypography.MetricMedium,
    titleLarge = ViraTypography.PageTitle,
    titleMedium = ViraTypography.ButtonLabel,
    titleSmall = ViraTypography.SectionTitle,
    bodyLarge = ViraTypography.Body,
    bodyMedium = ViraTypography.BodySecondary,
    labelLarge = ViraTypography.ButtonLabel,
    labelSmall = ViraTypography.Caption
)
