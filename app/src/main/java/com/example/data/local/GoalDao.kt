package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goal WHERE deletedAt IS NULL AND isActive = 1 LIMIT 1")
    fun getActiveGoal(): Flow<GoalEntity?>

    @Query("SELECT * FROM goal WHERE deletedAt IS NULL ORDER BY id DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("UPDATE goal SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncState = 'PENDING_DELETE' WHERE id = :id")
    suspend fun markDeleted(id: Int, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM goal WHERE id = :id")
    suspend fun deleteGoalById(id: Int)
}
