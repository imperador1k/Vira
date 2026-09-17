package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class ThemePreferencesRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("app_theme_mode")

    val themeMode: Flow<AppThemeMode> = context.dataStore.data
        .map { preferences ->
            val modeName = preferences[themeKey] ?: AppThemeMode.SYSTEM.name
            try {
                AppThemeMode.valueOf(modeName)
            } catch (e: IllegalArgumentException) {
                AppThemeMode.SYSTEM
            }
        }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = mode.name
        }
    }
}
