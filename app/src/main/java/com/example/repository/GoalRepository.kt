package com.example.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.GoalEntity
import com.example.data.local.OutboxEntityType
import com.example.data.local.OutboxOperationType
import com.example.data.local.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class GoalRepository(
    private val database: AppDatabase
) {
    private val goalDao = database.goalDao()
    private val syncOutboxDao = database.syncOutboxDao()

    fun getActiveGoal(): Flow<GoalEntity?> = goalDao.getActiveGoal()

    fun getAllGoals(): Flow<List<GoalEntity>> = goalDao.getAllGoals()

    suspend fun setGoal(goal: GoalEntity): Long {
        return database.withTransaction {
            val remoteId = goal.remoteId ?: java.util.UUID.randomUUID().toString()
            val entityToSave = goal.copy(
                remoteId = remoteId,
                syncState = SyncState.PENDING_UPLOAD.name,
                updatedAt = System.currentTimeMillis()
            )
            val generatedId = goalDao.insertGoal(entityToSave)

            syncOutboxDao.enqueueCoalesced(
                entityType = OutboxEntityType.GOAL.name,
                entityRemoteId = remoteId,
                operationType = OutboxOperationType.UPSERT.name,
                isLocallyCreatedOnly = (entityToSave.remoteVersion == 0L)
            )

            generatedId
        }
    }

    suspend fun updateGoal(goal: GoalEntity) {
        database.withTransaction {
            val remoteId = goal.remoteId ?: java.util.UUID.randomUUID().toString()
            val entityToSave = goal.copy(
                remoteId = remoteId,
                syncState = SyncState.PENDING_UPLOAD.name,
                updatedAt = System.currentTimeMillis()
            )
            goalDao.updateGoal(entityToSave)

            syncOutboxDao.enqueueCoalesced(
                entityType = OutboxEntityType.GOAL.name,
                entityRemoteId = remoteId,
                operationType = OutboxOperationType.UPSERT.name,
                isLocallyCreatedOnly = (entityToSave.remoteVersion == 0L)
            )
        }
    }

    suspend fun deleteGoalById(id: Int) {
        database.withTransaction {
            val goals = goalDao.getAllGoals().firstOrNull() ?: return@withTransaction
            val goal = goals.find { it.id == id } ?: return@withTransaction
            val isLocallyCreatedOnly = (goal.remoteVersion == 0L)
            val remoteId = goal.remoteId ?: return@withTransaction

            goalDao.markDeleted(id)

            syncOutboxDao.enqueueCoalesced(
                entityType = OutboxEntityType.GOAL.name,
                entityRemoteId = remoteId,
                operationType = OutboxOperationType.DELETE.name,
                isLocallyCreatedOnly = isLocallyCreatedOnly
            )
        }
    }
}
