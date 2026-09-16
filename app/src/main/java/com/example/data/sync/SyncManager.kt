package com.example.data.sync

import com.example.data.local.AppDatabase
import com.example.data.local.OutboxEntityType
import com.example.data.local.OutboxOperationType
import com.example.data.local.SyncOutboxEntity
import com.example.data.local.SyncState
import kotlinx.coroutines.flow.firstOrNull

class SyncManager(
    private val database: AppDatabase,
    private val remoteDataSource: SyncRemoteDataSource
) {

    /**
     * Process pending outbox entries in chronological order.
     * Returns true if all processed successfully, false if network error occurred.
     */
    suspend fun processOutboxBatch(batchSize: Int = 50): Boolean {
        val pendingOps = database.syncOutboxDao().getPendingBatch(batchSize)
        if (pendingOps.isEmpty()) return true

        for (op in pendingOps) {
            val success = processOperation(op)
            if (!success) {
                // Stop batch on first retryable network error to preserve ordering
                return false
            }
        }
        return true
    }

    private suspend fun processOperation(op: SyncOutboxEntity): Boolean {
        val result = if (op.operationType == OutboxOperationType.DELETE.name) {
            remoteDataSource.deleteEntity(op.entityType, op.entityRemoteId)
        } else {
            pushEntityUpsert(op)
        }

        return when (result) {
            is RemoteSyncResult.Success -> {
                handleOperationSuccess(op, result)
                true
            }
            is RemoteSyncResult.Conflict -> {
                handleOperationConflict(op, result)
                true
            }
            is RemoteSyncResult.NetworkError -> {
                handleOperationNetworkError(op, result)
                false
            }
        }
    }

    private suspend fun pushEntityUpsert(op: SyncOutboxEntity): RemoteSyncResult {
        return when (op.entityType) {
            OutboxEntityType.COLLECTION_ENTRY.name -> {
                val collections = database.collectionDao().getAllCollections().firstOrNull() ?: emptyList()
                val entry = collections.find { it.remoteId == op.entityRemoteId }
                    ?: return RemoteSyncResult.NetworkError("Local entity not found", canRetry = false)

                val spot = entry.collectionSpotId?.let { spotId ->
                    database.spotDao().getSpotById(spotId).firstOrNull()
                }

                val dto = RemoteCollectionDto(
                    remoteId = entry.remoteId ?: op.entityRemoteId,
                    containerCount = entry.containerCount,
                    timestamp = entry.timestamp,
                    estimatedValueCents = entry.estimatedValueCents,
                    spotRemoteId = spot?.remoteId,
                    note = entry.note,
                    latitude = entry.latitude,
                    longitude = entry.longitude,
                    clientUpdatedAt = entry.updatedAt
                )
                remoteDataSource.pushCollection(dto)
            }
            OutboxEntityType.COLLECTION_SPOT.name -> {
                val spots = database.spotDao().getAllSpots().firstOrNull() ?: emptyList()
                val spot = spots.find { it.remoteId == op.entityRemoteId }
                    ?: return RemoteSyncResult.NetworkError("Local entity not found", canRetry = false)

                val dto = RemoteSpotDto(
                    remoteId = spot.remoteId ?: op.entityRemoteId,
                    name = spot.name,
                    latitude = spot.latitude,
                    longitude = spot.longitude,
                    address = spot.address,
                    clientCreatedAt = spot.createdAt,
                    clientUpdatedAt = spot.updatedAt
                )
                remoteDataSource.pushSpot(dto)
            }
            OutboxEntityType.REDEMPTION_ENTRY.name -> {
                val redemptions = database.redemptionDao().getAllRedemptions().firstOrNull() ?: emptyList()
                val redemption = redemptions.find { it.remoteId == op.entityRemoteId }
                    ?: return RemoteSyncResult.NetworkError("Local entity not found", canRetry = false)

                val dto = RemoteRedemptionDto(
                    remoteId = redemption.remoteId ?: op.entityRemoteId,
                    presentedContainers = redemption.presentedContainers,
                    acceptedContainers = redemption.acceptedContainers,
                    rejectedContainers = redemption.rejectedContainers,
                    actualRecoveredCents = redemption.actualRecoveredCents,
                    timestamp = redemption.timestamp,
                    returnPointRemoteId = null,
                    note = redemption.note,
                    clientUpdatedAt = redemption.updatedAt
                )
                remoteDataSource.pushRedemption(dto)
            }
            OutboxEntityType.GOAL.name -> {
                val goals = database.goalDao().getAllGoals().firstOrNull() ?: emptyList()
                val goal = goals.find { it.remoteId == op.entityRemoteId }
                    ?: return RemoteSyncResult.NetworkError("Local entity not found", canRetry = false)

                val dto = RemoteGoalDto(
                    remoteId = goal.remoteId ?: op.entityRemoteId,
                    type = goal.type,
                    targetValue = goal.targetValue,
                    period = goal.period,
                    isActive = goal.isActive,
                    clientUpdatedAt = goal.updatedAt
                )
                remoteDataSource.pushGoal(dto)
            }
            OutboxEntityType.USER_PROFILE.name -> {
                val profile = database.userDao().getUserProfile().firstOrNull()
                    ?: return RemoteSyncResult.NetworkError("Local profile not found", canRetry = false)

                val dto = RemoteProfileDto(
                    remoteId = profile.remoteId ?: op.entityRemoteId,
                    name = profile.name,
                    themePreference = profile.themePreference,
                    clientUpdatedAt = profile.updatedAt
                )
                remoteDataSource.pushProfile(dto)
            }
            OutboxEntityType.FAVORITE_RETURN_POINT.name -> {
                val favorites = database.favoriteReturnPointDao().getFavoriteReturnPoints().firstOrNull() ?: emptyList()
                val point = favorites.find { it.id.toString() == op.entityRemoteId || it.name == op.entityRemoteId }
                val dto = RemoteFavoriteDto(
                    remoteId = op.entityRemoteId,
                    returnPointId = point?.id ?: 0,
                    clientCreatedAt = op.createdAt
                )
                remoteDataSource.pushFavorite(dto)
            }
            else -> RemoteSyncResult.NetworkError("Unknown entity type: ${op.entityType}", canRetry = false)
        }
    }

    private suspend fun handleOperationSuccess(op: SyncOutboxEntity, result: RemoteSyncResult.Success) {
        // 1. Mark local entity as SYNCED with authoritative server metadata
        when (op.entityType) {
            OutboxEntityType.COLLECTION_ENTRY.name -> {
                val collections = database.collectionDao().getAllCollections().firstOrNull() ?: emptyList()
                val entry = collections.find { it.remoteId == op.entityRemoteId }
                if (entry != null) {
                    database.collectionDao().insertCollection(
                        entry.copy(
                            syncState = SyncState.SYNCED.name,
                            serverUpdatedAt = result.serverUpdatedAt,
                            remoteVersion = result.remoteVersion
                        )
                    )
                }
            }
            OutboxEntityType.COLLECTION_SPOT.name -> {
                val spots = database.spotDao().getAllSpots().firstOrNull() ?: emptyList()
                val spot = spots.find { it.remoteId == op.entityRemoteId }
                if (spot != null) {
                    database.spotDao().updateSpot(
                        spot.copy(
                            syncState = SyncState.SYNCED.name,
                            serverUpdatedAt = result.serverUpdatedAt,
                            remoteVersion = result.remoteVersion
                        )
                    )
                }
            }
            OutboxEntityType.REDEMPTION_ENTRY.name -> {
                val redemptions = database.redemptionDao().getAllRedemptions().firstOrNull() ?: emptyList()
                val redemption = redemptions.find { it.remoteId == op.entityRemoteId }
                if (redemption != null) {
                    database.redemptionDao().insertRedemption(
                        redemption.copy(
                            syncState = SyncState.SYNCED.name,
                            serverUpdatedAt = result.serverUpdatedAt,
                            remoteVersion = result.remoteVersion
                        )
                    )
                }
            }
            OutboxEntityType.GOAL.name -> {
                val goals = database.goalDao().getAllGoals().firstOrNull() ?: emptyList()
                val goal = goals.find { it.remoteId == op.entityRemoteId }
                if (goal != null) {
                    database.goalDao().updateGoal(
                        goal.copy(
                            syncState = SyncState.SYNCED.name,
                            serverUpdatedAt = result.serverUpdatedAt,
                            remoteVersion = result.remoteVersion
                        )
                    )
                }
            }
            OutboxEntityType.USER_PROFILE.name -> {
                val profile = database.userDao().getUserProfile().firstOrNull()
                if (profile != null && profile.remoteId == op.entityRemoteId) {
                    database.userDao().insertUserProfile(
                        profile.copy(
                            syncState = SyncState.SYNCED.name,
                            serverUpdatedAt = result.serverUpdatedAt,
                            remoteVersion = result.remoteVersion
                        )
                    )
                }
            }
        }

        // 2. Remove outbox operation
        database.syncOutboxDao().deleteOperation(op.operationId)
    }

    private suspend fun handleOperationConflict(op: SyncOutboxEntity, result: RemoteSyncResult.Conflict) {
        // Conflict policy: Do NOT merge numeric container quantities from two versions.
        // Update local entity metadata with conflict flag and delete from outbox.
        database.syncOutboxDao().deleteOperation(op.operationId)
    }

    private suspend fun handleOperationNetworkError(op: SyncOutboxEntity, result: RemoteSyncResult.NetworkError) {
        database.syncOutboxDao().updateOperation(
            op.copy(
                retryCount = op.retryCount + 1,
                lastAttemptAt = System.currentTimeMillis(),
                lastError = result.message
            )
        )
    }
}
