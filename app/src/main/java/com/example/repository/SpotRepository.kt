package com.example.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.CollectionSpotEntity
import com.example.data.local.OutboxEntityType
import com.example.data.local.OutboxOperationType
import com.example.data.local.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class SpotRepository(
    private val database: AppDatabase
) {
    private val spotDao = database.spotDao()
    private val syncOutboxDao = database.syncOutboxDao()

    fun getAllSpots(): Flow<List<CollectionSpotEntity>> = spotDao.getAllSpots()

    fun getSpotById(id: Int): Flow<CollectionSpotEntity?> = spotDao.getSpotById(id)

    suspend fun insertSpot(spot: CollectionSpotEntity): Long {
        return database.withTransaction {
            val remoteId = spot.remoteId ?: java.util.UUID.randomUUID().toString()
            val entityToSave = spot.copy(
                remoteId = remoteId,
                syncState = SyncState.PENDING_UPLOAD.name,
                updatedAt = System.currentTimeMillis()
            )
            val generatedId = spotDao.insertSpot(entityToSave)

            syncOutboxDao.enqueueCoalesced(
                entityType = OutboxEntityType.COLLECTION_SPOT.name,
                entityRemoteId = remoteId,
                operationType = OutboxOperationType.UPSERT.name,
                isLocallyCreatedOnly = (entityToSave.remoteVersion == 0L)
            )

            generatedId
        }
    }

    suspend fun updateSpot(spot: CollectionSpotEntity) {
        database.withTransaction {
            val remoteId = spot.remoteId ?: java.util.UUID.randomUUID().toString()
            val entityToSave = spot.copy(
                remoteId = remoteId,
                syncState = SyncState.PENDING_UPLOAD.name,
                updatedAt = System.currentTimeMillis()
            )
            spotDao.updateSpot(entityToSave)

            syncOutboxDao.enqueueCoalesced(
                entityType = OutboxEntityType.COLLECTION_SPOT.name,
                entityRemoteId = remoteId,
                operationType = OutboxOperationType.UPSERT.name,
                isLocallyCreatedOnly = (entityToSave.remoteVersion == 0L)
            )
        }
    }

    suspend fun updateSpotStats(
        spotId: Int,
        totalVisits: Int,
        lifetimeContainers: Int,
        averageContainersPerVisit: Double,
        lastVisitedAt: Long?
    ) {
        database.withTransaction {
            spotDao.updateSpotStats(
                id = spotId,
                totalVisits = totalVisits,
                lifetimeContainers = lifetimeContainers,
                averageContainersPerVisit = averageContainersPerVisit,
                lastVisitedAt = lastVisitedAt
            )
            val spot = spotDao.getSpotById(spotId).firstOrNull()
            if (spot != null) {
                val remoteId = spot.remoteId ?: return@withTransaction
                syncOutboxDao.enqueueCoalesced(
                    entityType = OutboxEntityType.COLLECTION_SPOT.name,
                    entityRemoteId = remoteId,
                    operationType = OutboxOperationType.UPSERT.name,
                    isLocallyCreatedOnly = (spot.remoteVersion == 0L)
                )
            }
        }
    }

    suspend fun deleteSpotById(id: Int) {
        database.withTransaction {
            val spot = spotDao.getSpotById(id).firstOrNull() ?: return@withTransaction
            val isLocallyCreatedOnly = (spot.remoteVersion == 0L)
            val remoteId = spot.remoteId ?: return@withTransaction

            spotDao.markDeleted(id)

            syncOutboxDao.enqueueCoalesced(
                entityType = OutboxEntityType.COLLECTION_SPOT.name,
                entityRemoteId = remoteId,
                operationType = OutboxOperationType.DELETE.name,
                isLocallyCreatedOnly = isLocallyCreatedOnly
            )
        }
    }
}
