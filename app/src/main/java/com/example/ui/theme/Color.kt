package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Vira Design System - Core Color Palette
 * 
 * Aesthetics: Light-First, Ceramic & Graphite Editorial, Precision Data, Restrained Cyan.
 * Rules:
 * - Light theme: Off-white background (#F8F9FA), crisp white cards (#FFFFFF), graphite text (#111827), slate grey (#6B7280).
 * - Dark theme: Mineral graphite (#0E1117), elevated card surfaces (#161B22), soft white text (#F0F6FC).
 * - Restrained Vira cyan accent for primary actions and key brand moments.
 * - Purpose-driven semantics: Emerald for success, Warm Amber for warnings, Coral Red for destructive actions.
 */

// Vira Cyan Accent (Signature Brand Identity)
val ViraCyan = Color(0xFF00BFA5)          // Light-mode primary cyan (restrained, high contrast)
val ViraCyanDark = Color(0xFF00E5C4)      // Dark-mode electric cyan (luminous)
val ViraCyanLight = Color(0xFF00897B)
val ViraCyanHover = Color(0xFF00A896)
val ViraCyanPressed = Color(0xFF00796B)
val ViraCyanMutedDark = Color(0xFF0E2E2A)
val ViraCyanMutedLight = Color(0xFFE6F7F5)
val ViraCyanGlow = Color(0x3300BFA5)

// Dark Theme - Deep Graphite Surface Hierarchy (Linear / Raycast tier)
val ViraBackgroundDark = Color(0xFF0E1117)
val ViraSurfaceDark = Color(0xFF161B22)
val ViraSurfaceElevatedDark = Color(0xFF21262D)
val ViraSurfaceInteractiveDark = Color(0xFF2D333B)
val ViraDividerDark = Color(0xFF30363D)
val ViraBorderDark = Color(0xFF30363D)

val ViraOnBackgroundDark = Color(0xFFF0F6FC)
val ViraOnSurfaceDark = Color(0xFFF0F6FC)
val ViraOnSurfaceVariantDark = Color(0xFF8B949E)
val ViraOutlineDark = Color(0xFF30363D)

// Light Theme - Off-White & Soft Ceramic Hierarchy (Light-First)
val ViraBackgroundLight = Color(0xFFF8F9FA)
val ViraSurfaceLight = Color(0xFFFFFFFF)
val ViraSurfaceElevatedLight = Color(0xFFF1F3F5)
val ViraSurfaceInteractiveLight = Color(0xFFE9ECEF)
val ViraDividerLight = Color(0xFFE5E7EB)
val ViraBorderLight = Color(0xFFE5E7EB)

val ViraOnBackgroundLight = Color(0xFF111827)
val ViraOnSurfaceLight = Color(0xFF111827)
val ViraOnSurfaceVariantLight = Color(0xFF6B7280)
val ViraOutlineLight = Color(0xFFE5E7EB)

// Functional / Semantic Colors
val ViraSuccess = Color(0xFF10B981)
val ViraWarning = Color(0xFFF59E0B)
val ViraError = Color(0xFFEF4444)
