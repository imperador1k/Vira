package com.example

import android.content.Context
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.repository.CollectionRepository
import com.example.repository.RedemptionRepository
import com.example.repository.SpotRepository
import com.example.domain.BalanceService

class AppContainer(private val context: Context) {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "vira_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    val collectionRepository: CollectionRepository by lazy {
        CollectionRepository(database.collectionDao())
    }

    val redemptionRepository: RedemptionRepository by lazy {
        RedemptionRepository(database.redemptionDao())
    }

    val spotRepository: SpotRepository by lazy {
        SpotRepository(database.spotDao())
    }

    val balanceService: BalanceService by lazy {
        BalanceService(collectionRepository, redemptionRepository)
    }
}
