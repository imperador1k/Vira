package com.example.data.preferences

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_profile_preferences")

data class UserProfileData(
    val displayName: String = "Utilizador Vira",
    val username: String = "",
    val city: String = "",
    val avatarFilePath: String? = null
)

class UserPreferencesRepository(private val context: Context) {

    private val keyDisplayName = stringPreferencesKey("profile_display_name")
    private val keyUsername = stringPreferencesKey("profile_username")
    private val keyCity = stringPreferencesKey("profile_city")
    private val keyAvatarPath = stringPreferencesKey("profile_avatar_path")
    private val keySessionCount = intPreferencesKey("app_session_count")
    private val keyLastReminderTime = longPreferencesKey("account_reminder_last_shown")
    private val keyReminderDismissedForever = booleanPreferencesKey("account_reminder_dismissed_forever")

    val profileData: Flow<UserProfileData> = context.userPrefsDataStore.data.map { prefs ->
        val avatarPath = prefs[keyAvatarPath]
        val validAvatarPath = if (avatarPath != null && File(avatarPath).exists()) avatarPath else null
        UserProfileData(
            displayName = prefs[keyDisplayName]?.takeIf { it.isNotBlank() } ?: "Utilizador Vira",
            username = prefs[keyUsername] ?: "",
            city = prefs[keyCity] ?: "",
            avatarFilePath = validAvatarPath
        )
    }

    val sessionCount: Flow<Int> = context.userPrefsDataStore.data.map { prefs ->
        prefs[keySessionCount] ?: 1
    }

    val lastReminderTime: Flow<Long> = context.userPrefsDataStore.data.map { prefs ->
        prefs[keyLastReminderTime] ?: 0L
    }

    val isReminderDismissedForever: Flow<Boolean> = context.userPrefsDataStore.data.map { prefs ->
        prefs[keyReminderDismissedForever] ?: false
    }

    suspend fun saveProfile(displayName: String, username: String, city: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[keyDisplayName] = displayName.trim()
            prefs[keyUsername] = username.trim().removePrefix("@")
            prefs[keyCity] = city.trim()
        }
    }

    suspend fun saveAvatarFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap == null) return@withContext false

            val avatarFile = File(context.filesDir, "profile_avatar.jpg")
            val outputStream = FileOutputStream(avatarFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()

            context.userPrefsDataStore.edit { prefs ->
                prefs[keyAvatarPath] = avatarFile.absolutePath
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeAvatar() = withContext(Dispatchers.IO) {
        try {
            val avatarFile = File(context.filesDir, "profile_avatar.jpg")
            if (avatarFile.exists()) {
                avatarFile.delete()
            }
            context.userPrefsDataStore.edit { prefs ->
                prefs.remove(keyAvatarPath)
            }
        } catch (_: Exception) {}
    }

    suspend fun incrementSessionCount() {
        context.userPrefsDataStore.edit { prefs ->
            val current = prefs[keySessionCount] ?: 0
            prefs[keySessionCount] = current + 1
        }
    }

    suspend fun snoozeAccountReminder() {
        context.userPrefsDataStore.edit { prefs ->
            prefs[keyLastReminderTime] = System.currentTimeMillis()
        }
    }

    suspend fun dismissAccountReminderForever() {
        context.userPrefsDataStore.edit { prefs ->
            prefs[keyReminderDismissedForever] = true
        }
    }
}
