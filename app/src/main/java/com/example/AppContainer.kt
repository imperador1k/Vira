package com.example

import android.content.Context
import androidx.room.Room
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthState
import com.example.data.auth.SupabaseAuthRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
            AppDatabase.MIGRATION_2_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_1_5,
            AppDatabase.MIGRATION_2_5,
            AppDatabase.MIGRATION_3_5
        )
        // Production safety: destructive migration completely removed to prevent data loss
        .build()
    }

    val datasetOwnershipManager: com.example.data.sync.DatasetOwnershipManager by lazy {
        com.example.data.sync.DatasetOwnershipManager(database.syncMetadataDao())
    }

    val authRepository: AuthRepository by lazy {
        SupabaseClientProvider.getClient()?.let { client ->
            SupabaseAuthRepository(client, datasetOwnershipManager)
        } ?: object : AuthRepository {
            private val _state = MutableStateFlow<AuthState>(AuthState.LocalOnly)
            override val authState: StateFlow<AuthState> = _state
            override suspend fun signIn(email: String, password: String): Result<Unit> =
                Result.failure(IllegalStateException("Supabase not configured"))
            override suspend fun signUp(email: String, password: String): Result<Unit> =
                Result.failure(IllegalStateException("Supabase not configured"))
            override suspend fun signOut(): Result<Unit> = Result.success(Unit)
            override suspend fun refreshAuthState() {}
            override fun getCurrentUserId(): String? = null
            override fun getCurrentEmail(): String? = null
        }
    }

    val syncRemoteDataSource: SyncRemoteDataSource by lazy {
        SupabaseClientProvider.getClient()?.let { client ->
            SupabaseSyncRemoteDataSource(client)
        } ?: FakeSyncRemoteDataSource()
    }

    val syncCursorManager: SyncCursorManager by lazy {
        SyncCursorManager(database.syncMetadataDao())
    }

    val syncManager: SyncManager by lazy {
        SyncManager(
            database = database,
            remoteDataSource = syncRemoteDataSource,
            cursorManager = syncCursorManager,
            authRepository = authRepository,
            ownershipManager = datasetOwnershipManager
        )
    }

    suspend fun clearPersonalDataAndBind(newUserId: String) {
        database.clearPersonalDatasetAndRebind(newUserId)
        authRepository.refreshAuthState()
        syncManager.syncAll()
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

    val networkMonitor: com.example.util.NetworkMonitor by lazy {
        com.example.util.NetworkMonitor(context)
    }

    val themePreferencesRepository: com.example.data.preferences.ThemePreferencesRepository by lazy {
        com.example.data.preferences.ThemePreferencesRepository(context)
    }

    val userPreferencesRepository: com.example.data.preferences.UserPreferencesRepository by lazy {
        com.example.data.preferences.UserPreferencesRepository(context)
    }

    fun scheduleBackgroundSync() {
        SyncWorker.scheduleSync(context)
        SyncWorker.schedulePeriodicSync(context)
    }
}
