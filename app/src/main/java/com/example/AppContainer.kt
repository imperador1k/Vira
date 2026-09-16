package com.example

import android.content.Context
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.sync.FakeSyncRemoteDataSource
import com.example.data.sync.SyncCursorManager
import com.example.data.sync.SyncManager
import com.example.data.sync.SyncRemoteDataSource
import com.example.data.sync.SyncWorker
import com.example.data.sync.supabase.SupabaseClientProvider
import com.example.data.sync.supabase.SupabaseSyncRemoteDataSource
import com.example.domain.BalanceService
import com.example.repository.AndroidLocationRepository
import com.example.repository.CollectionRepository
import com.example.repository.GoalRepository
import com.example.repository.LocationRepository
import com.example.repository.RedemptionRepository
import com.example.repository.ReturnPointRepository
import com.example.repository.SpotRepository
import com.example.repository.UserRepository

class AppContainer(private val context: Context) {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "vira_database"
        )
        .addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_1_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_1_4,
            AppDatabase.MIGRATION_2_4
        )
        // Production safety: destructive migration completely removed to prevent data loss
        .build()
    }

    val syncRemoteDataSource: SyncRemoteDataSource by lazy {
        SupabaseClientProvider.getClient()?.let { client ->
            SupabaseSyncRemoteDataSource(client)
        } ?: FakeSyncRemoteDataSource()
    }

    val syncCursorManager: SyncCursorManager by lazy {
        SyncCursorManager(context)
    }

    val syncManager: SyncManager by lazy {
        SyncManager(database, syncRemoteDataSource, syncCursorManager)
    }

    val collectionRepository: CollectionRepository by lazy {
        CollectionRepository(database)
    }

    val redemptionRepository: RedemptionRepository by lazy {
        RedemptionRepository(database)
    }

    val spotRepository: SpotRepository by lazy {
        SpotRepository(database)
    }

    val returnPointRepository: ReturnPointRepository by lazy {
        ReturnPointRepository(database)
    }

    val goalRepository: GoalRepository by lazy {
        GoalRepository(database)
    }

    val userRepository: UserRepository by lazy {
        UserRepository(database) {
            database.clearAllTables()
        }
    }

    val balanceService: BalanceService by lazy {
        BalanceService(collectionRepository, redemptionRepository)
    }

    val locationRepository: LocationRepository by lazy {
        AndroidLocationRepository(context)
    }

    fun scheduleBackgroundSync() {
        SyncWorker.scheduleSync(context)
    }
}
