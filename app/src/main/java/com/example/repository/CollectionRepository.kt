package com.example.repository

import com.example.data.local.CollectionDao
import com.example.data.local.CollectionEntryEntity
import kotlinx.coroutines.flow.Flow

class CollectionRepository(private val collectionDao: CollectionDao) {
    fun getAllCollections(): Flow<List<CollectionEntryEntity>> = collectionDao.getAllCollections()

    suspend fun insertCollection(entry: CollectionEntryEntity): Long =
        collectionDao.insertCollection(entry)

    suspend fun deleteCollectionById(id: Int) =
        collectionDao.deleteCollectionById(id)
}
