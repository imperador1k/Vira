package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "redemption_entry")
data class RedemptionEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val presentedContainers: Int,
    val acceptedContainers: Int,
    val rejectedContainers: Int,
    val actualRecoveredCents: Long,
    val timestamp: Long,
    val returnPointId: Int? = null,
    val note: String? = null,
    val createdAt: Long = timestamp,
    val updatedAt: Long = timestamp,
    val remoteId: String? = UUID.randomUUID().toString(),
    val deletedAt: Long? = null,
    val syncState: String = SyncState.LOCAL_ONLY.name,
    val serverUpdatedAt: Long? = null,
    val remoteVersion: Long = 0L
)
