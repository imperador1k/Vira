package com.example.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteReturnPointEntity
import com.example.data.local.OutboxEntityType
import com.example.data.local.OutboxOperationType
import com.example.data.local.ReturnPointEntity
import com.example.data.local.ReturnPointSource
import com.example.data.local.ReturnPointType
import com.example.data.local.VerificationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ReturnPointRepository(
    private val database: AppDatabase
) {
    private val returnPointDao = database.returnPointDao()
    private val favoriteReturnPointDao = database.favoriteReturnPointDao()
    private val syncOutboxDao = database.syncOutboxDao()

    fun getAllReturnPoints(): Flow<List<ReturnPointEntity>> =
        returnPointDao.getAllReturnPoints()

    fun getVerifiedReturnPoints(): Flow<List<ReturnPointEntity>> =
        returnPointDao.getVerifiedReturnPoints()

    fun getCommunityPoints(): Flow<List<ReturnPointEntity>> =
        returnPointDao.getCommunityPoints()

    fun getReturnPointById(id: Int): Flow<ReturnPointEntity?> =
        returnPointDao.getReturnPointById(id)

    fun getFavoriteReturnPoints(): Flow<List<ReturnPointEntity>> =
        favoriteReturnPointDao.getFavoriteReturnPoints()

    fun isFavorite(returnPointId: Int): Flow<Boolean> =
        favoriteReturnPointDao.isFavorite(returnPointId)

    suspend fun toggleFavorite(returnPointId: Int) {
        database.withTransaction {
            val currentlyFavorite = favoriteReturnPointDao.isFavorite(returnPointId).firstOrNull() ?: false
            if (currentlyFavorite) {
                favoriteReturnPointDao.deleteFavorite(returnPointId)
                syncOutboxDao.enqueueCoalesced(
                    entityType = OutboxEntityType.FAVORITE_RETURN_POINT.name,
                    entityRemoteId = returnPointId.toString(),
                    operationType = OutboxOperationType.DELETE.name,
                    isLocallyCreatedOnly = false
                )
            } else {
                val fav = FavoriteReturnPointEntity(returnPointId = returnPointId)
                favoriteReturnPointDao.insertFavorite(fav)
                syncOutboxDao.enqueueCoalesced(
                    entityType = OutboxEntityType.FAVORITE_RETURN_POINT.name,
                    entityRemoteId = returnPointId.toString(),
                    operationType = OutboxOperationType.UPSERT.name,
                    isLocallyCreatedOnly = true
                )
            }
        }
    }

    suspend fun insertReturnPoint(point: ReturnPointEntity): Long =
        returnPointDao.insertReturnPoint(point)

    suspend fun deleteReturnPointById(id: Int) =
        returnPointDao.deleteReturnPointById(id)

    suspend fun seedDefaultReturnPointsIfEmpty() {
        // In personal beta: Do NOT fabricate or seed fake return points.
        // Purge any legacy demo points so that an honest empty state is presented.
        returnPointDao.clearAll()
    }
}
