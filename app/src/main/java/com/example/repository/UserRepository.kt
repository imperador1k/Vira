package com.example.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.LocalUserProfileEntity
import com.example.data.local.OutboxEntityType
import com.example.data.local.OutboxOperationType
import com.example.data.local.SyncState
import kotlinx.coroutines.flow.Flow

class UserRepository(
    private val database: AppDatabase,
    private val clearAllDataCallback: suspend () -> Unit
) {
    private val userDao = database.userDao()
    private val syncOutboxDao = database.syncOutboxDao()

    fun getUserProfile(): Flow<LocalUserProfileEntity?> = userDao.getUserProfile()

    suspend fun saveUserProfile(profile: LocalUserProfileEntity) {
        database.withTransaction {
            val remoteId = profile.remoteId ?: java.util.UUID.randomUUID().toString()
            val entityToSave = profile.copy(
                remoteId = remoteId,
                syncState = SyncState.PENDING_UPLOAD.name,
                updatedAt = System.currentTimeMillis()
            )
            userDao.insertUserProfile(entityToSave)

            syncOutboxDao.enqueueCoalesced(
                entityType = OutboxEntityType.USER_PROFILE.name,
                entityRemoteId = remoteId,
                operationType = OutboxOperationType.UPSERT.name,
                isLocallyCreatedOnly = (entityToSave.remoteVersion == 0L)
            )
        }
    }

    suspend fun clearAllUserData() {
        database.withTransaction {
            syncOutboxDao.clearAll()
            clearAllDataCallback()
        }
    }
}
