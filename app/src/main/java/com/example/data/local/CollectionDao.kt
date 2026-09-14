package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collection_entry ORDER BY timestamp DESC")
    fun getAllCollections(): Flow<List<CollectionEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(entry: CollectionEntryEntity): Long

    @Query("DELETE FROM collection_entry WHERE id = :id")
    suspend fun deleteCollectionById(id: Int)

    // Spots
    @Query("SELECT * FROM collection_spot ORDER BY totalVisits DESC")
    fun getAllSpots(): Flow<List<CollectionSpotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpot(spot: CollectionSpotEntity): Long

    @Query("DELETE FROM collection_spot WHERE id = :id")
    suspend fun deleteSpotById(id: Int)
}
