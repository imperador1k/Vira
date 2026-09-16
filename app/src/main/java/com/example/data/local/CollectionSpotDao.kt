package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionSpotDao {
    @Query("SELECT * FROM collection_spot WHERE deletedAt IS NULL ORDER BY totalVisits DESC")
    fun getAllSpots(): Flow<List<CollectionSpotEntity>>

    @Query("SELECT * FROM collection_spot WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun getSpotById(id: Int): Flow<CollectionSpotEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpot(spot: CollectionSpotEntity): Long

    @Update
    suspend fun updateSpot(spot: CollectionSpotEntity)

    @Query("""
        UPDATE collection_spot 
        SET totalVisits = :totalVisits,
            lifetimeContainers = :lifetimeContainers,
            averageContainersPerVisit = :averageContainersPerVisit,
            lastVisitedAt = :lastVisitedAt,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateSpotStats(
        id: Int,
        totalVisits: Int,
        lifetimeContainers: Int,
        averageContainersPerVisit: Double,
        lastVisitedAt: Long?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE collection_spot SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncState = 'PENDING_DELETE' WHERE id = :id")
    suspend fun markDeleted(id: Int, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM collection_spot WHERE id = :id")
    suspend fun deleteSpotById(id: Int)
}
