package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RedemptionDao {
    @Query("SELECT * FROM redemption_entry ORDER BY timestamp DESC")
    fun getAllRedemptions(): Flow<List<RedemptionEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRedemption(entry: RedemptionEntryEntity): Long

    @Query("DELETE FROM redemption_entry WHERE id = :id")
    suspend fun deleteRedemptionById(id: Int)
}
