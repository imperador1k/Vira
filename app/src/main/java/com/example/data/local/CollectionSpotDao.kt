package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionSpotDao {
    @Query("SELECT * FROM collection_spot ORDER BY totalVisits DESC")
    fun getAllSpots(): Flow<List<CollectionSpotEntity>>

    @Query("SELECT * FROM collection_spot WHERE id = :id LIMIT 1")
    fun getSpotById(id: Int): Flow<CollectionSpotEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpot(spot: CollectionSpotEntity)

    @Query("DELETE FROM collection_spot WHERE id = :id")
    suspend fun deleteSpotById(id: Int)
}
