package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReturnPointDao {

    @Query("SELECT * FROM return_point WHERE deletedAt IS NULL AND verificationStatus != 'REMOVED' ORDER BY name ASC")
    fun getAllReturnPoints(): Flow<List<ReturnPointEntity>>

    @Query("SELECT * FROM return_point WHERE deletedAt IS NULL AND verificationStatus = 'VERIFIED' ORDER BY name ASC")
    fun getVerifiedReturnPoints(): Flow<List<ReturnPointEntity>>

    @Query("SELECT * FROM return_point WHERE deletedAt IS NULL AND (verificationStatus = 'COMMUNITY_CONFIRMED' OR verificationStatus = 'COMMUNITY_SUBMITTED') ORDER BY name ASC")
    fun getCommunityPoints(): Flow<List<ReturnPointEntity>>

    @Query("SELECT * FROM return_point WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun getReturnPointById(id: Int): Flow<ReturnPointEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnPoint(point: ReturnPointEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<ReturnPointEntity>)

    @Query("UPDATE return_point SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncState = 'PENDING_DELETE' WHERE id = :id")
    suspend fun markDeleted(id: Int, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM return_point WHERE id = :id")
    suspend fun deleteReturnPointById(id: Int)
}
