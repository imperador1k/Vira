package com.example.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.CollectionEntryEntity
import com.example.data.local.OutboxEntityType
import com.example.data.local.OutboxOperationType
import com.example.data.local.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class CollectionRepository(
    private val database: AppDatabase
) {
    private val collectionDao = database.collectionDao()
    private val syncOutboxDao = database.syncOutboxDao()

    fun getAllCollections(): Flow<List<CollectionEntryEntity>> = collectionDao.getAllCollections()

    fun getCollectionsBySpotId(spotId: Int): Flow<List<CollectionEntryEntity>> =
        collectionDao.getCollectionsBySpotId(spotId)

    fun getCollectionById(id: Int): Flow<CollectionEntryEntity?> =
        collectionDao.getCollectionById(id)

    suspend fun insertCollection(entry: CollectionEntryEntity): Long {
        return database.withTransaction {
            val remoteId = entry.remoteId ?: java.util.UUID.randomUUID().toString()
            val entityToSave = entry.copy(
                remoteId = remoteId,
                syncState = SyncState.PENDING_UPLOAD.name,
                updatedAt = System.currentTimeMillis()
            )
            val generatedId = collectionDao.insertCollection(entityToSave)

            syncOutboxDao.enqueueCoalesced(
                entityType = OutboxEntityType.COLLECTION_ENTRY.name,
                entityRemoteId = remoteId,
                operationType = OutboxOperationType.UPSERT.name,
                isLocallyCreatedOnly = (entityToSave.remoteVersion == 0L)
            )

            generatedId
        }
    }

    suspend fun updateCollection(id: Int, newCount: Int, newNote: String?) {
        val existing = collectionDao.getCollectionById(id).firstOrNull() ?: return
        val updated = existing.copy(
            containerCount = newCount,
            estimatedValueCents = newCount * com.example.util.Constants.DEPOSIT_VALUE_CENTS,
            note = newNote,
            updatedAt = System.currentTimeMillis()
        )
        insertCollection(updated)
    }

    suspend fun deleteCollectionById(id: Int) {
        database.withTransaction {
            val entry = collectionDao.getCollectionById(id).firstOrNull() ?: return@withTransaction
            val isLocallyCreatedOnly = (entry.remoteVersion == 0L)
            val remoteId = entry.remoteId ?: return@withTransaction

            collectionDao.markDeleted(id)

            syncOutboxDao.enqueueCoalesced(
                entityType = OutboxEntityType.COLLECTION_ENTRY.name,
                entityRemoteId = remoteId,
                operationType = OutboxOperationType.DELETE.name,
                isLocallyCreatedOnly = isLocallyCreatedOnly
            )
        }
    }
}
