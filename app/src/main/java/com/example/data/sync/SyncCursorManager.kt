package com.example.data.sync

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persistence of the remote synchronization cursor.
 * The cursor corresponds to the highest authoritative server_version processed by the client.
 */
class SyncCursorManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getCursor(): Long {
        return prefs.getLong(KEY_CURSOR, 0L)
    }

    fun setCursor(cursor: Long) {
        prefs.edit().putLong(KEY_CURSOR, cursor).apply()
    }

    fun clearCursor() {
        prefs.edit().remove(KEY_CURSOR).apply()
    }

    companion object {
        private const val PREFS_NAME = "vira_sync_cursor_prefs"
        private const val KEY_CURSOR = "sync_cursor_version"
    }
}
