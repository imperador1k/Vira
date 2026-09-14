package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collection_spot")
data class CollectionSpotEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val createdAt: Long,
    val lastVisitedAt: Long?,
    val totalVisits: Int,
    val lifetimeContainers: Int,
    val averageContainersPerVisit: Double
)
