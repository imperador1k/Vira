package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteReturnPointDao {

    @Query("""
        SELECT return_point.* FROM return_point
        INNER JOIN favorite_return_point ON return_point.id = favorite_return_point.returnPointId
        WHERE return_point.deletedAt IS NULL AND favorite_return_point.deletedAt IS NULL
        ORDER BY return_point.name ASC
    """)
    fun getFavoriteReturnPoints(): Flow<List<ReturnPointEntity>>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM favorite_return_point 
            WHERE returnPointId = :returnPointId AND deletedAt IS NULL
        )
    """)
    fun isFavorite(returnPointId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteReturnPointEntity)

    @Query("UPDATE favorite_return_point SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncState = 'PENDING_DELETE' WHERE returnPointId = :returnPointId")
    suspend fun markDeleted(returnPointId: Int, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM favorite_return_point WHERE returnPointId = :returnPointId")
    suspend fun deleteFavorite(returnPointId: Int)
}
