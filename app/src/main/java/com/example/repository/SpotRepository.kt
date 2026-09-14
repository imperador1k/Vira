package com.example.repository

import com.example.data.local.CollectionSpotDao
import com.example.data.local.CollectionSpotEntity
import kotlinx.coroutines.flow.Flow

class SpotRepository(private val spotDao: CollectionSpotDao) {

    fun getAllSpots(): Flow<List<CollectionSpotEntity>> = spotDao.getAllSpots()

    fun getSpotById(id: Int): Flow<CollectionSpotEntity?> = spotDao.getSpotById(id)

    suspend fun insertSpot(spot: CollectionSpotEntity) {
        spotDao.insertSpot(spot)
    }

    suspend fun deleteSpotById(id: Int) {
        spotDao.deleteSpotById(id)
    }
}
