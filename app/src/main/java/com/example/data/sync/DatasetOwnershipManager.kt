package com.example.data.sync

import com.example.data.local.SyncMetadataDao
import com.example.data.local.SyncMetadataEntity

data class LocalAccountBinding(
    val ownerUserId: String?,
    val linkedAt: Long?,
    val bindingVersion: Int = 1
)

/**
 * Manages persistent ownership of the local Room dataset.
 * Semantics:
 * - ownerUserId == null: unowned/local-only dataset (accountless mode)
 * - ownerUserId == currentUserId: dataset belongs to authenticated user
 * - ownerUserId != currentUserId: account mismatch
 */
class DatasetOwnershipManager(
    private val syncMetadataDao: SyncMetadataDao
) {
    suspend fun getOwnerUserId(): String? {
        return syncMetadataDao.getValue(KEY_DATASET_OWNER_ID)
    }

    suspend fun getLinkedAt(): Long? {
        return syncMetadataDao.getValue(KEY_DATASET_LINKED_AT)?.toLongOrNull()
    }

    suspend fun getBinding(): LocalAccountBinding {
        val ownerId = getOwnerUserId()
        val linkedAt = getLinkedAt()
        val version = syncMetadataDao.getValue(KEY_BINDING_VERSION)?.toIntOrNull() ?: 1
        return LocalAccountBinding(
            ownerUserId = ownerId,
            linkedAt = linkedAt,
            bindingVersion = version
        )
    }

    suspend fun bindOwner(userId: String, timestamp: Long = System.currentTimeMillis()) {
        syncMetadataDao.setValue(
            SyncMetadataEntity(
                key = KEY_DATASET_OWNER_ID,
                value = userId,
                updatedAt = timestamp
            )
        )
        syncMetadataDao.setValue(
            SyncMetadataEntity(
                key = KEY_DATASET_LINKED_AT,
                value = timestamp.toString(),
                updatedAt = timestamp
            )
        )
        syncMetadataDao.setValue(
            SyncMetadataEntity(
                key = KEY_BINDING_VERSION,
                value = "1",
                updatedAt = timestamp
            )
        )
    }

    suspend fun clearOwner() {
        syncMetadataDao.deleteKey(KEY_DATASET_OWNER_ID)
        syncMetadataDao.deleteKey(KEY_DATASET_LINKED_AT)
        syncMetadataDao.deleteKey(KEY_BINDING_VERSION)
    }

    suspend fun isOwnedBy(userId: String?): Boolean {
        if (userId == null) return false
        val currentOwner = getOwnerUserId() ?: return false
        return currentOwner == userId
    }

    companion object {
        const val KEY_DATASET_OWNER_ID = "dataset_owner_user_id"
        const val KEY_DATASET_LINKED_AT = "dataset_linked_at"
        const val KEY_BINDING_VERSION = "dataset_binding_version"
    }
}
