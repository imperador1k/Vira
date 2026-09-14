package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "redemption_entry")
data class RedemptionEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val presentedContainers: Int,
    val acceptedContainers: Int,
    val rejectedContainers: Int,
    val actualRecoveredCents: Long,
    val timestamp: Long,
    val returnPointId: Int?,
    val note: String?
)
