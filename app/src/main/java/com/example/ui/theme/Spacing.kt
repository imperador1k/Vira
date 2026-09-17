package com.example.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Vira Design System - Strict Spacing Scale & Radius Tokens
 * 
 * Scale: 4, 8, 12, 16, 24, 32, 48, 64 dp
 * Eliminates arbitrary ad-hoc padding across all screens.
 */
object ViraSpacing {
    val none: Dp = 0.dp
    val space4: Dp = 4.dp
    val space8: Dp = 8.dp
    val space12: Dp = 12.dp
    val space16: Dp = 16.dp
    val space24: Dp = 24.dp
    val space32: Dp = 32.dp
    val space48: Dp = 48.dp
    val space64: Dp = 64.dp
}

/**
 * Backward-compatible Spacing alias
 */
object Spacing {
    val none = ViraSpacing.none
    val extraSmall = ViraSpacing.space4
    val small = ViraSpacing.space8
    val mediumSmall = ViraSpacing.space12
    val medium = ViraSpacing.space16
    val large = ViraSpacing.space24
    val extraLarge = ViraSpacing.space32
    val huge = ViraSpacing.space48
    val massive = ViraSpacing.space64
}

/**
 * Vira Corner Radius Scale
 * 
 * small controls / chips: 10-12dp
 * buttons: 16dp
 * cards: 22dp
 * sheets: 28dp
 * pills: 999dp
 */
object ViraRadius {
    val small: Dp = 10.dp
    val medium: Dp = 16.dp
    val large: Dp = 22.dp
    val sheet: Dp = 28.dp
    val pill: Dp = 999.dp
}

/**
 * Vira Icon Size Scale
 */
object ViraIconSize {
    val small: Dp = 16.dp
    val medium: Dp = 20.dp
    val normal: Dp = 24.dp
    val large: Dp = 32.dp
    val hero: Dp = 48.dp
}
