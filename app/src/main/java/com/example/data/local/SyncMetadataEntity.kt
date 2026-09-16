package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Key-value metadata table for synchronization engine.
 * Enables ACID crash consistency by keeping sync cursor state within the SQLite database.
 */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)
