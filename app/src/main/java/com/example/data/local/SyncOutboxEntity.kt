package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class OutboxOperationType {
    UPSERT,
    DELETE
}

enum class OutboxEntityType {
    COLLECTION_ENTRY,
    REDEMPTION_ENTRY,
    COLLECTION_SPOT,
    GOAL,
    USER_PROFILE,
    FAVORITE_RETURN_POINT
}

/**
 * Represents a durable pending sync operation in Room.
 * Ensures offline changes cannot disappear if the application or network fails.
 */
@Entity(tableName = "sync_outbox")
data class SyncOutboxEntity(
    @PrimaryKey val operationId: String = UUID.randomUUID().toString(),
    val entityType: String,
    val entityRemoteId: String,
    val operationType: String, // UPSERT, DELETE
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val lastError: String? = null,
    val payloadVersion: Int = 1,
    val payloadJson: String? = null
)
