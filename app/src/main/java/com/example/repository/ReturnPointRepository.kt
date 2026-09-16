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
        val existing = returnPointDao.getAllReturnPoints().firstOrNull()
        if (existing.isNullOrEmpty()) {
            val defaults = listOf(
                ReturnPointEntity(
                    id = 1,
                    name = "Pingo Doce Baixa (Máquina SDR)",
                    latitude = 38.7118,
                    longitude = -9.1382,
                    type = ReturnPointType.AUTOMATIC_MACHINE.name,
                    source = ReturnPointSource.OFFICIAL.name,
                    verificationStatus = VerificationStatus.VERIFIED.name,
                    lastVerifiedAt = System.currentTimeMillis(),
                    address = "Rua do Ouro 120, Lisboa",
                    openingHours = "08:00 - 21:00"
                ),
                ReturnPointEntity(
                    id = 2,
                    name = "Continente Bom Dia Chiado",
                    latitude = 38.7105,
                    longitude = -9.1417,
                    type = ReturnPointType.AUTOMATIC_MACHINE.name,
                    source = ReturnPointSource.OFFICIAL.name,
                    verificationStatus = VerificationStatus.VERIFIED.name,
                    lastVerifiedAt = System.currentTimeMillis(),
                    address = "Largo do Chiado 8, Lisboa",
                    openingHours = "08:30 - 21:30"
                ),
                ReturnPointEntity(
                    id = 3,
                    name = "Ponto Comunitário Rossio",
                    latitude = 38.7142,
                    longitude = -9.1396,
                    type = ReturnPointType.MANUAL_POINT.name,
                    source = ReturnPointSource.COMMUNITY.name,
                    verificationStatus = VerificationStatus.COMMUNITY_CONFIRMED.name,
                    lastVerifiedAt = null,
                    address = "Praça Dom Pedro IV, Lisboa",
                    openingHours = "Horário comercial"
                )
            )
            returnPointDao.insertAll(defaults)
        }
    }
}
