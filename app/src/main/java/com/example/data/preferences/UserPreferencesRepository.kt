package com.example.data.preferences

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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

    fun decodeAndFixOrientation(uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val rawBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (rawBitmap == null) return null

            val exifStream = context.contentResolver.openInputStream(uri)
            val orientation = if (exifStream != null) {
                val exif = ExifInterface(exifStream)
                val orient = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                exifStream.close()
                orient
            } else ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return rawBitmap
            }
            val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            if (rotated != rawBitmap) {
                rawBitmap.recycle()
            }
            rotated
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveAvatarFromBitmap(bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
        try {
            context.filesDir.listFiles { _, name -> name.startsWith("avatar_") || name == "profile_avatar.jpg" }
                ?.forEach { it.delete() }

            val avatarFile = File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(avatarFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, outputStream)
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

    suspend fun saveAvatarFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val bitmap = decodeAndFixOrientation(uri) ?: return@withContext false
        val size = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        val square = Bitmap.createBitmap(bitmap, x, y, size, size)
        val scaled = Bitmap.createScaledBitmap(square, 512, 512, true)
        if (square != bitmap && square != scaled) square.recycle()
        if (scaled != bitmap) bitmap.recycle()
        saveAvatarFromBitmap(scaled)
    }

    suspend fun removeAvatar() = withContext(Dispatchers.IO) {
        try {
            context.filesDir.listFiles { _, name -> name.startsWith("avatar_") || name == "profile_avatar.jpg" }
                ?.forEach { it.delete() }
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
