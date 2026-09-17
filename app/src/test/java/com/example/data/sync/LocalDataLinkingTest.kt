package com.example.data.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthState
import com.example.data.local.AppDatabase
import com.example.data.local.CollectionEntryEntity
import com.example.data.local.CollectionSpotEntity
import com.example.data.local.GoalEntity
import com.example.data.local.LocalUserProfileEntity
import com.example.data.local.OutboxEntityType
import com.example.data.local.RedemptionEntryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class LocalDataLinkingTest {

    private lateinit var database: AppDatabase
    private lateinit var fakeRemote: FakeSyncRemoteDataSource
    private lateinit var syncManager: SyncManager

    private val authStateFlow = MutableStateFlow<AuthState>(AuthState.LocalOnly)
    private var currentUserId: String? = null

    private val fakeAuthRepo = object : AuthRepository {
        override val authState: StateFlow<AuthState> = authStateFlow
        override suspend fun signIn(email: String, password: String): Result<Unit> {
            currentUserId = "user-123"
            authStateFlow.value = AuthState.Authenticated(currentUserId!!, email)
            return Result.success(Unit)
        }
        override suspend fun signUp(email: String, password: String): Result<Unit> = signIn(email, password)
        override suspend fun signOut(): Result<Unit> {
            currentUserId = null
            authStateFlow.value = AuthState.LocalOnly
            return Result.success(Unit)
        }
        override suspend fun refreshAuthState() {}
        override fun getCurrentUserId(): String? = currentUserId
        override fun getCurrentEmail(): String? = if (currentUserId != null) "test@example.com" else null
    }

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        fakeRemote = FakeSyncRemoteDataSource()
        val cursorManager = SyncCursorManager(database.syncMetadataDao())
        val ownershipManager = DatasetOwnershipManager(database.syncMetadataDao())
        syncManager = SyncManager(
            database = database,
            remoteDataSource = fakeRemote,
            cursorManager = cursorManager,
            authRepository = fakeAuthRepo,
            ownershipManager = ownershipManager
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun unauthenticatedUser_doesNotTriggerRemoteSync() = runTest {
        // Given offline data
        val collectionId = UUID.randomUUID().toString()
        database.collectionDao().insertCollection(
            CollectionEntryEntity(
                remoteId = collectionId,
                containerCount = 10,
                timestamp = 1000L,
                estimatedValueCents = 100L,
                remoteVersion = 0L
            )
        )

        // When syncing in LocalOnly mode
        val success = syncManager.syncAll()

        // Then sync returns true (skips remote work) without contacting remote
        assertEquals(true, success)
        assertEquals(0, fakeRemote.pushedCollections.size)
    }

    @Test
    fun linkingExistingLocalData_enqueuesOutboxAndPreservesRemoteIdWithoutDuplicates() = runTest {
        // Given existing local data created before login
        val collectionId = UUID.randomUUID().toString()
        val spotId = UUID.randomUUID().toString()

        database.collectionDao().insertCollection(
            CollectionEntryEntity(
                remoteId = collectionId,
                containerCount = 15,
                timestamp = 2000L,
                estimatedValueCents = 150L,
                remoteVersion = 0L
            )
        )
        database.spotDao().insertSpot(
            CollectionSpotEntity(
                remoteId = spotId,
                name = "Local Spot",
                latitude = 40.0,
                longitude = -8.0,
                createdAt = 2000L,
                remoteVersion = 0L
            )
        )

        // When user creates account / signs in
        fakeAuthRepo.signIn("user@vira.app", "Secret123!")
        fakeRemote.currentUserId = "user-123"

        // Trigger sync
        val syncResult = syncManager.syncAll()
        assertEquals(true, syncResult)

        // Verify data was uploaded to remote with exact same remoteIds
        assertEquals(1, fakeRemote.pushedCollections.size)
        assertEquals(collectionId, fakeRemote.pushedCollections[0].remoteId)
        assertEquals(15, fakeRemote.pushedCollections[0].containerCount)

        assertEquals(1, fakeRemote.pushedSpots.size)
        assertEquals(spotId, fakeRemote.pushedSpots[0].remoteId)

        // Verify outbox was completely processed and cleared
        val pending = database.syncOutboxDao().getPendingBatch(50)
        assertEquals(0, pending.size)

        // Verify running syncAll again does NOT duplicate records
        syncManager.syncAll()
        assertEquals(1, fakeRemote.pushedCollections.size)
        assertEquals(1, fakeRemote.pushedSpots.size)
    }
}
