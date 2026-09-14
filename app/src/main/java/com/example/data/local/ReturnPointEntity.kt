package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "return_point")
data class ReturnPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // AUTOMATIC_MACHINE, KIOSK, MANUAL_POINT, USER_ADDED, UNKNOWN
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val source: String,
    val isVerified: Boolean,
    val lastUpdatedTimestamp: Long,
    val userNotes: String?
)
