package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "user_profile")
data class LocalUserProfileEntity(
    @PrimaryKey val id: Int = 1, // Only one user profile
    val name: String,
    val memberSince: Long,
    val monthlyGoalId: Int?,
    val themePreference: String,
    val remoteId: String? = UUID.randomUUID().toString(),
    val updatedAt: Long = memberSince,
    val deletedAt: Long? = null,
    val syncState: String = SyncState.LOCAL_ONLY.name,
    val serverUpdatedAt: Long? = null,
    val remoteVersion: Long = 0L
)
