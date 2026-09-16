package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "favorite_return_point")
data class FavoriteReturnPointEntity(
    @PrimaryKey
    val returnPointId: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val remoteId: String? = UUID.randomUUID().toString(),
    val deletedAt: Long? = null,
    val syncState: String = SyncState.LOCAL_ONLY.name,
    val serverUpdatedAt: Long? = null,
    val remoteVersion: Long = 0L
)
