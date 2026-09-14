package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
      primary = ViraCyanDark,
      onPrimary = Color.Black,
      primaryContainer = ViraCyanDark.copy(alpha = 0.2f),
      onPrimaryContainer = ViraCyanDark,
      background = ViraBackgroundDark,
      onBackground = ViraOnBackgroundDark,
      surface = ViraSurfaceDark,
      onSurface = ViraOnSurfaceDark,
      surfaceVariant = ViraSurfaceVariantDark,
      onSurfaceVariant = ViraOnSurfaceVariantDark,
      outline = ViraOutlineDark
  )

private val LightColorScheme =
  lightColorScheme(
      primary = ViraCyanLight,
      onPrimary = Color.White,
      primaryContainer = ViraCyanLight.copy(alpha = 0.1f),
      onPrimaryContainer = ViraCyanLight,
      background = ViraBackgroundLight,
      onBackground = ViraOnBackgroundLight,
      surface = ViraSurfaceLight,
      onSurface = ViraOnSurfaceLight,
      surfaceVariant = ViraSurfaceVariantLight,
      onSurfaceVariant = ViraOnSurfaceVariantLight,
      outline = ViraOutlineLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
