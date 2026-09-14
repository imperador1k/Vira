package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collection_entry")
data class CollectionEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val containerCount: Int,
    val timestamp: Long,
    val estimatedValueCents: Long,
    val collectionSpotId: Int?, // Optional linked spot
    val note: String?,
    val latitude: Double?,
    val longitude: Double?
)
