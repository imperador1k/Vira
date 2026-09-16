package com.example.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.OutboxEntityType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SupabaseSecurityIsolationTest {

    private lateinit var databaseA: AppDatabase
    private lateinit var cursorManagerA: SyncCursorManager
    private lateinit var syncManagerA: SyncManager

    private lateinit var databaseB: AppDatabase
    private lateinit var cursorManagerB: SyncCursorManager
    private lateinit var syncManagerB: SyncManager

    private lateinit var sharedRemoteDataSource: FakeSyncRemoteDataSource

    private val userA = "user-a-uuid-1111"
    private val userB = "user-b-uuid-2222"

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Instance A local database & cursor
        databaseA = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cursorManagerA = SyncCursorManager(databaseA.syncMetadataDao())
        cursorManagerA.clearCursor()

        // Instance B local database & cursor
        databaseB = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cursorManagerB = SyncCursorManager(databaseB.syncMetadataDao())
        cursorManagerB.clearCursor()

        // Shared server remote data source
        sharedRemoteDataSource = FakeSyncRemoteDataSource()

        // Sync manager for Instance A (authenticated as USER_A)
        sharedRemoteDataSource.currentUserId = userA
        syncManagerA = SyncManager(databaseA, sharedRemoteDataSource, cursorManagerA)

        // Sync manager for Instance B (authenticated as USER_B)
        sharedRemoteDataSource.currentUserId = userB
        syncManagerB = SyncManager(databaseB, sharedRemoteDataSource, cursorManagerB)
    }

    @After
    fun tearDown() = runBlocking {
        databaseA.close()
        databaseB.close()
    }

    @Test
    fun multiUserPullIsolation_UserA_NeverReceives_UserB_Data() = runBlocking {
        // 1. Seed records for USER_A
        sharedRemoteDataSource.currentUserId = userA
        sharedRemoteDataSource.pushCollection(
            RemoteCollectionDto(remoteId = "col-a-1", containerCount = 10, timestamp = 1000L, estimatedValueCents = 100L)
        )
        sharedRemoteDataSource.pushSpot(
            RemoteSpotDto(remoteId = "spot-a-1", name = "Spot A", latitude = 38.7, longitude = -9.1, clientCreatedAt = 1000L, clientUpdatedAt = 1000L)
        )
        sharedRemoteDataSource.pushRedemption(
            RemoteRedemptionDto(remoteId = "red-a-1", presentedContainers = 5, acceptedContainers = 5, actualRecoveredCents = 50L, timestamp = 1000L)
        )
        sharedRemoteDataSource.pushGoal(
            RemoteGoalDto(remoteId = "goal-a-1", type = "MONTHLY", targetValue = 50, period = "MONTHLY", isActive = true, clientUpdatedAt = 1000L)
        )

        // 2. Seed records for USER_B
        sharedRemoteDataSource.currentUserId = userB
        sharedRemoteDataSource.pushCollection(
            RemoteCollectionDto(remoteId = "col-b-1", containerCount = 20, timestamp = 2000L, estimatedValueCents = 200L)
        )
        sharedRemoteDataSource.pushSpot(
            RemoteSpotDto(remoteId = "spot-b-1", name = "Spot B", latitude = 41.1, longitude = -8.6, clientCreatedAt = 2000L, clientUpdatedAt = 2000L)
        )
        sharedRemoteDataSource.pushRedemption(
            RemoteRedemptionDto(remoteId = "red-b-1", presentedContainers = 10, acceptedContainers = 10, actualRecoveredCents = 100L, timestamp = 2000L)
        )
        sharedRemoteDataSource.pushGoal(
            RemoteGoalDto(remoteId = "goal-b-1", type = "MONTHLY", targetValue = 100, period = "MONTHLY", isActive = true, clientUpdatedAt = 2000L)
        )

        // 3. USER_A executes pull
        sharedRemoteDataSource.currentUserId = userA
        val successA = syncManagerA.pullAndReconcile()
        assertTrue("Pull for USER_A must succeed", successA)

        // ASSERT: USER_A receives only A records, exactly ZERO B records
        val collectionsA = databaseA.collectionDao().getAllCollections().first()
        val spotsA = databaseA.spotDao().getAllSpots().first()
        val redemptionsA = databaseA.redemptionDao().getAllRedemptions().first()
        val goalsA = databaseA.goalDao().getAllGoals().first()

        assertEquals(1, collectionsA.size)
        assertEquals("col-a-1", collectionsA.first().remoteId)
        assertTrue("USER_A must NOT contain any USER_B collection", collectionsA.none { it.remoteId == "col-b-1" })

        assertEquals(1, spotsA.size)
        assertEquals("spot-a-1", spotsA.first().remoteId)
        assertTrue("USER_A must NOT contain any USER_B spot", spotsA.none { it.remoteId == "spot-b-1" })

        assertEquals(1, redemptionsA.size)
        assertEquals("red-a-1", redemptionsA.first().remoteId)
        assertTrue("USER_A must NOT contain any USER_B redemption", redemptionsA.none { it.remoteId == "red-b-1" })

        assertEquals(1, goalsA.size)
        assertEquals("goal-a-1", goalsA.first().remoteId)
        assertTrue("USER_A must NOT contain any USER_B goal", goalsA.none { it.remoteId == "goal-b-1" })

        // 4. USER_B executes pull
        sharedRemoteDataSource.currentUserId = userB
        val successB = syncManagerB.pullAndReconcile()
        assertTrue("Pull for USER_B must succeed", successB)

        // ASSERT: USER_B receives only B records, exactly ZERO A records
        val collectionsB = databaseB.collectionDao().getAllCollections().first()
        val spotsB = databaseB.spotDao().getAllSpots().first()
        val redemptionsB = databaseB.redemptionDao().getAllRedemptions().first()
        val goalsB = databaseB.goalDao().getAllGoals().first()

        assertEquals(1, collectionsB.size)
        assertEquals("col-b-1", collectionsB.first().remoteId)
        assertTrue("USER_B must NOT contain any USER_A collection", collectionsB.none { it.remoteId == "col-a-1" })

        assertEquals(1, spotsB.size)
        assertEquals("spot-b-1", spotsB.first().remoteId)
        assertTrue("USER_B must NOT contain any USER_A spot", spotsB.none { it.remoteId == "spot-a-1" })

        assertEquals(1, redemptionsB.size)
        assertEquals("red-b-1", redemptionsB.first().remoteId)
        assertTrue("USER_B must NOT contain any USER_A redemption", redemptionsB.none { it.remoteId == "red-a-1" })

        assertEquals(1, goalsB.size)
        assertEquals("goal-b-1", goalsB.first().remoteId)
        assertTrue("USER_B must NOT contain any USER_A goal", goalsB.none { it.remoteId == "goal-a-1" })
    }

    @Test
    fun directTableAttackTests_CrossUserMutation_RejectedByRLS() = runBlocking {
        // Seed an existing entity belonging to USER_B
        sharedRemoteDataSource.currentUserId = userB
        val seedRes = sharedRemoteDataSource.pushCollection(
            RemoteCollectionDto(remoteId = "col-victim-b", containerCount = 50, timestamp = 1000L, estimatedValueCents = 500L)
        )
        assertTrue(seedRes is RemoteSyncResult.Success)

        // Attack 1: USER_A attempts to update USER_B's collection
        sharedRemoteDataSource.currentUserId = userA
        val attackUpdate = sharedRemoteDataSource.pushCollection(
            RemoteCollectionDto(remoteId = "col-victim-b", containerCount = 999, timestamp = 2000L, estimatedValueCents = 9990L)
        )
        assertTrue("Cross-user UPDATE must be rejected by RLS", attackUpdate is RemoteSyncResult.NetworkError)
        assertEquals("RLS Violation: user cannot mutate another user's collection", (attackUpdate as RemoteSyncResult.NetworkError).message)

        // Verify entity was NOT modified on server
        val pristine = sharedRemoteDataSource.pushedCollections.first { it.remoteId == "col-victim-b" }
        assertEquals(50, pristine.containerCount)

        // Attack 2: USER_A attempts to delete USER_B's collection
        val attackDelete = sharedRemoteDataSource.deleteEntity(OutboxEntityType.COLLECTION_ENTRY.name, "col-victim-b")
        assertTrue("Cross-user DELETE must be rejected by RLS", attackDelete is RemoteSyncResult.NetworkError)
        assertEquals("RLS Violation: cannot delete entity belonging to another user", (attackDelete as RemoteSyncResult.NetworkError).message)

        // Verify entity was NOT deleted on server
        assertTrue(sharedRemoteDataSource.pushedCollections.any { it.remoteId == "col-victim-b" })
    }

    @Test
    fun anonymousCaller_DeniedAccessToSyncTablesAndRPC() = runBlocking {
        // Seed entity as USER_A
        sharedRemoteDataSource.currentUserId = userA
        sharedRemoteDataSource.pushCollection(
            RemoteCollectionDto(remoteId = "col-private-a", containerCount = 10, timestamp = 1000L, estimatedValueCents = 100L)
        )

        // Set session to anonymous (null)
        sharedRemoteDataSource.currentUserId = null

        // 1. Anonymous Pull must fail immediately with authentication error
        var pullExceptionThrown = false
        try {
            sharedRemoteDataSource.pullChanges(0L)
        } catch (e: IllegalStateException) {
            pullExceptionThrown = true
            assertTrue(e.message?.contains("Unauthorized: authentication required") == true)
        }
        assertTrue("Anonymous pull must throw Unauthorized exception", pullExceptionThrown)

        // 2. Anonymous Push must be rejected
        val anonPush = sharedRemoteDataSource.pushCollection(
            RemoteCollectionDto(remoteId = "col-anon-leak", containerCount = 1, timestamp = 1L, estimatedValueCents = 10L)
        )
        assertTrue("Anonymous push must fail", anonPush is RemoteSyncResult.NetworkError)
        assertEquals("Unauthorized: authentication required", (anonPush as RemoteSyncResult.NetworkError).message)
    }
}
