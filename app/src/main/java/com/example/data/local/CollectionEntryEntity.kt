package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "collection_entry")
data class CollectionEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val containerCount: Int,
    val timestamp: Long,
    val estimatedValueCents: Long,
    val collectionSpotId: Int? = null, // Optional linked spot
    val note: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Long = timestamp,
    val updatedAt: Long = timestamp,
    val remoteId: String? = UUID.randomUUID().toString(),
    val deletedAt: Long? = null,
    val syncState: String = SyncState.LOCAL_ONLY.name,
    val serverUpdatedAt: Long? = null,
    val remoteVersion: Long = 0L
)
