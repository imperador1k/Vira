package com.example.data.sync

import com.example.data.local.SyncMetadataDao
import com.example.data.local.SyncMetadataEntity

/**
 * Manages persistence of the remote synchronization cursor backed by Room sync_metadata.
 * Guarantees ACID crash consistency when executed within Room database transactions.
 */
class SyncCursorManager(
    private val syncMetadataDao: SyncMetadataDao
) {

    suspend fun getCursor(): Long {
        return syncMetadataDao.getValue(KEY_CURSOR)?.toLongOrNull() ?: 0L
    }

    suspend fun setCursor(cursor: Long) {
        syncMetadataDao.setValue(
            SyncMetadataEntity(
                key = KEY_CURSOR,
                value = cursor.toString(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearCursor() {
        syncMetadataDao.deleteKey(KEY_CURSOR)
    }

    companion object {
        const val KEY_CURSOR = "sync_cursor_version"
    }
}
