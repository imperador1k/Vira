package com.example.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.OutboxEntityType
import com.example.data.local.OutboxOperationType
import com.example.data.local.RedemptionEntryEntity
import com.example.data.local.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class RedemptionRepository(
    private val database: AppDatabase
) {
    private val redemptionDao = database.redemptionDao()
    private val syncOutboxDao = database.syncOutboxDao()

    fun getAllRedemptions(): Flow<List<RedemptionEntryEntity>> = redemptionDao.getAllRedemptions()

    suspend fun insertRedemption(entry: RedemptionEntryEntity): Long {
        return database.withTransaction {
            val remoteId = entry.remoteId ?: java.util.UUID.randomUUID().toString()
            val entityToSave = entry.copy(
                remoteId = remoteId,
                syncState = SyncState.PENDING_UPLOAD.name,
                updatedAt = System.currentTimeMillis()
            )
            val generatedId = redemptionDao.insertRedemption(entityToSave)

            syncOutboxDao.enqueueCoalesced(
                entityType = OutboxEntityType.REDEMPTION_ENTRY.name,
                entityRemoteId = remoteId,
                operationType = OutboxOperationType.UPSERT.name,
                isLocallyCreatedOnly = (entityToSave.remoteVersion == 0L)
            )

            generatedId
        }
    }

    suspend fun deleteRedemptionById(id: Int) {
        database.withTransaction {
            val entry = redemptionDao.getRedemptionById(id).firstOrNull() ?: return@withTransaction
            val isLocallyCreatedOnly = (entry.remoteVersion == 0L)
            val remoteId = entry.remoteId ?: return@withTransaction

            redemptionDao.markDeleted(id)

            syncOutboxDao.enqueueCoalesced(
                entityType = OutboxEntityType.REDEMPTION_ENTRY.name,
                entityRemoteId = remoteId,
                operationType = OutboxOperationType.DELETE.name,
                isLocallyCreatedOnly = isLocallyCreatedOnly
            )
        }
    }
}
