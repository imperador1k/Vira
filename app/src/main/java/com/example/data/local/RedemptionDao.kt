package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RedemptionDao {
    @Query("SELECT * FROM redemption_entry WHERE deletedAt IS NULL ORDER BY timestamp DESC")
    fun getAllRedemptions(): Flow<List<RedemptionEntryEntity>>

    @Query("SELECT * FROM redemption_entry WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun getRedemptionById(id: Int): Flow<RedemptionEntryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRedemption(entry: RedemptionEntryEntity): Long

    @Query("UPDATE redemption_entry SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncState = 'PENDING_DELETE' WHERE id = :id")
    suspend fun markDeleted(id: Int, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM redemption_entry WHERE id = :id")
    suspend fun deleteRedemptionById(id: Int)
}
