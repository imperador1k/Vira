package com.example.repository

import com.example.data.local.RedemptionDao
import com.example.data.local.RedemptionEntryEntity
import kotlinx.coroutines.flow.Flow

class RedemptionRepository(private val redemptionDao: RedemptionDao) {
    fun getAllRedemptions(): Flow<List<RedemptionEntryEntity>> = redemptionDao.getAllRedemptions()

    suspend fun insertRedemption(entry: RedemptionEntryEntity): Long =
        redemptionDao.insertRedemption(entry)

    suspend fun deleteRedemptionById(id: Int) =
        redemptionDao.deleteRedemptionById(id)
}
