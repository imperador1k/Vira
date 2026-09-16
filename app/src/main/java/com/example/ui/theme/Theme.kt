package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ViraExtraColors(
    val surfaceElevated: Color,
    val surfaceInteractive: Color,
    val divider: Color,
    val cyanMuted: Color
)

val LocalViraExtraColors = staticCompositionLocalOf {
    ViraExtraColors(
        surfaceElevated = ViraSurfaceElevatedDark,
        surfaceInteractive = ViraSurfaceInteractiveDark,
        divider = ViraDividerDark,
        cyanMuted = ViraCyanMutedDark
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = ViraCyan,
    onPrimary = Color(0xFF0C0E12),
    primaryContainer = ViraCyanMutedDark,
    onPrimaryContainer = ViraCyanDark,
    background = ViraBackgroundDark,
    onBackground = ViraOnBackgroundDark,
    surface = ViraSurfaceDark,
    onSurface = ViraOnSurfaceDark,
    surfaceVariant = ViraSurfaceElevatedDark,
    onSurfaceVariant = ViraOnSurfaceVariantDark,
    surfaceContainer = ViraSurfaceElevatedDark,
    surfaceContainerHigh = ViraSurfaceInteractiveDark,
    outline = ViraDividerDark,
    outlineVariant = ViraOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = ViraCyanLight,
    onPrimary = Color.White,
    primaryContainer = ViraCyanMutedLight,
    onPrimaryContainer = ViraCyanLight,
    background = ViraBackgroundLight,
    onBackground = ViraOnBackgroundLight,
    surface = ViraSurfaceLight,
    onSurface = ViraOnSurfaceLight,
    surfaceVariant = ViraSurfaceElevatedLight,
    onSurfaceVariant = ViraOnSurfaceVariantLight,
    surfaceContainer = ViraSurfaceElevatedLight,
    surfaceContainerHigh = ViraSurfaceInteractiveLight,
    outline = ViraDividerLight,
    outlineVariant = ViraOutlineLight
)

@Composable
fun ViraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extraColors = if (darkTheme) {
        ViraExtraColors(
            surfaceElevated = ViraSurfaceElevatedDark,
            surfaceInteractive = ViraSurfaceInteractiveDark,
            divider = ViraDividerDark,
            cyanMuted = ViraCyanMutedDark
        )
    } else {
        ViraExtraColors(
            surfaceElevated = ViraSurfaceElevatedLight,
            surfaceInteractive = ViraSurfaceInteractiveLight,
            divider = ViraDividerLight,
            cyanMuted = ViraCyanMutedLight
        )
    }

    CompositionLocalProvider(LocalViraExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/**
 * Backward-compatible alias for existing callers
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    ViraTheme(darkTheme = darkTheme, content = content)
}
