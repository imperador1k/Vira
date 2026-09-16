package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * CollectionSpot represents a private place where THIS USER has personally collected containers.
 * Remains strictly private by default.
 * Statistics (totalVisits, lifetimeContainers, averageContainersPerVisit) are derived from collection history.
 */
@Entity(tableName = "collection_spot")
data class CollectionSpotEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastVisitedAt: Long? = null,
    val totalVisits: Int = 0,
    val lifetimeContainers: Int = 0,
    val averageContainersPerVisit: Double = 0.0,
    val remoteId: String? = UUID.randomUUID().toString(),
    val deletedAt: Long? = null,
    val syncState: String = SyncState.LOCAL_ONLY.name,
    val serverUpdatedAt: Long? = null,
    val remoteVersion: Long = 0L
)
