package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "goal")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // CONTAINERS, VALUE
    val targetValue: Int, // containers count or cents
    val period: String, // MONTHLY, WEEKLY
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val remoteId: String? = UUID.randomUUID().toString(),
    val deletedAt: Long? = null,
    val syncState: String = SyncState.LOCAL_ONLY.name,
    val serverUpdatedAt: Long? = null,
    val remoteVersion: Long = 0L
)
