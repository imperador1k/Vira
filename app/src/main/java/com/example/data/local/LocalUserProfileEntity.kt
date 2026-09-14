package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class LocalUserProfileEntity(
    @PrimaryKey val id: Int = 1, // Only one user profile
    val name: String,
    val memberSince: Long,
    val monthlyGoalId: Int?,
    val themePreference: String
)
