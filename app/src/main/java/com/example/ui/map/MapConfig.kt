package com.example.ui.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

object MapConfig {
    const val STYLE_LIGHT = "https://tiles.openfreemap.org/styles/positron"
    const val STYLE_DARK = "https://tiles.openfreemap.org/styles/dark"

    @Composable
    fun getStyleUrl(): String {
        return if (isSystemInDarkTheme()) STYLE_DARK else STYLE_LIGHT
    }
}
