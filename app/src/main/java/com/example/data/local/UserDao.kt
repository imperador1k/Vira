package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 AND deletedAt IS NULL LIMIT 1")
    fun getUserProfile(): Flow<LocalUserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: LocalUserProfileEntity)

    @Query("UPDATE user_profile SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncState = 'PENDING_DELETE' WHERE id = :id")
    suspend fun markDeleted(id: Int, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM user_profile")
    suspend fun clearUserProfile()
}
