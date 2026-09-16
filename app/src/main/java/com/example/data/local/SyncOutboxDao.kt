package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface SyncOutboxDao {

    @Query("SELECT * FROM sync_outbox ORDER BY createdAt ASC")
    fun observePendingOperations(): Flow<List<SyncOutboxEntity>>

    @Query("SELECT * FROM sync_outbox ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingBatch(limit: Int = 50): List<SyncOutboxEntity>

    @Query("SELECT * FROM sync_outbox WHERE entityRemoteId = :remoteId LIMIT 1")
    suspend fun getPendingByRemoteId(remoteId: String): SyncOutboxEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: SyncOutboxEntity): Long

    @Update
    suspend fun updateOperation(operation: SyncOutboxEntity)

    @Query("DELETE FROM sync_outbox WHERE operationId = :operationId")
    suspend fun deleteOperation(operationId: String)

    @Query("DELETE FROM sync_outbox WHERE entityRemoteId = :entityRemoteId")
    suspend fun deleteByRemoteId(entityRemoteId: String)

    @Query("DELETE FROM sync_outbox")
    suspend fun clearAll()

    /**
     * Atomically enqueues an operation with deterministic coalescing:
     * - UPSERT + UPSERT on the same entity collapses to the latest UPSERT.
     * - LOCAL_ONLY record (never synced, remoteVersion == 0) + DELETE removes any pending UPSERT and does NOT enqueue DELETE.
     * - Previously synced record (remoteVersion > 0) + DELETE collapses to a single DELETE.
     */
    @Transaction
    suspend fun enqueueCoalesced(
        entityType: String,
        entityRemoteId: String,
        operationType: String,
        payloadJson: String? = null,
        isLocallyCreatedOnly: Boolean = false
    ) {
        val existing = getPendingByRemoteId(entityRemoteId)

        if (operationType == OutboxOperationType.DELETE.name) {
            if (isLocallyCreatedOnly) {
                // Record never existed remotely; eliminate pending operation entirely (no-op remote sync)
                if (existing != null) {
                    deleteOperation(existing.operationId)
                }
                return
            }

            if (existing != null) {
                updateOperation(
                    existing.copy(
                        operationType = OutboxOperationType.DELETE.name,
                        payloadJson = null,
                        retryCount = 0,
                        lastError = null,
                        createdAt = System.currentTimeMillis()
                    )
                )
            } else {
                insertOperation(
                    SyncOutboxEntity(
                        operationId = UUID.randomUUID().toString(),
                        entityType = entityType,
                        entityRemoteId = entityRemoteId,
                        operationType = OutboxOperationType.DELETE.name,
                        payloadJson = null,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        } else {
            // UPSERT operation
            if (existing != null && existing.operationType == OutboxOperationType.UPSERT.name) {
                // Collapse into the latest UPSERT
                updateOperation(
                    existing.copy(
                        payloadJson = payloadJson,
                        retryCount = 0,
                        lastError = null,
                        createdAt = System.currentTimeMillis()
                    )
                )
            } else if (existing != null && existing.operationType == OutboxOperationType.DELETE.name) {
                // If previously marked deleted, update to latest UPSERT
                updateOperation(
                    existing.copy(
                        operationType = OutboxOperationType.UPSERT.name,
                        payloadJson = payloadJson,
                        retryCount = 0,
                        lastError = null,
                        createdAt = System.currentTimeMillis()
                    )
                )
            } else {
                insertOperation(
                    SyncOutboxEntity(
                        operationId = UUID.randomUUID().toString(),
                        entityType = entityType,
                        entityRemoteId = entityRemoteId,
                        operationType = OutboxOperationType.UPSERT.name,
                        payloadJson = payloadJson,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
