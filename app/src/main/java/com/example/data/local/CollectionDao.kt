package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collection_entry WHERE deletedAt IS NULL ORDER BY timestamp DESC")
    fun getAllCollections(): Flow<List<CollectionEntryEntity>>

    @Query("SELECT * FROM collection_entry WHERE collectionSpotId = :spotId AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun getCollectionsBySpotId(spotId: Int): Flow<List<CollectionEntryEntity>>

    @Query("SELECT * FROM collection_entry WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun getCollectionById(id: Int): Flow<CollectionEntryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(entry: CollectionEntryEntity): Long

    @Query("UPDATE collection_entry SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncState = 'PENDING_DELETE' WHERE id = :id")
    suspend fun markDeleted(id: Int, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM collection_entry WHERE id = :id")
    suspend fun deleteCollectionById(id: Int)
}
