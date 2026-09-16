package com.example.data.sync

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.OutboxEntityType
import com.example.data.local.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SyncVersioningProtocolTest {

    private lateinit var database: AppDatabase
    private lateinit var cursorManager: SyncCursorManager
    private lateinit var fakeRemoteDataSource: FakeSyncRemoteDataSource
    private lateinit var syncManager: SyncManager

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cursorManager = SyncCursorManager(database.syncMetadataDao())
        cursorManager.clearCursor()
        fakeRemoteDataSource = FakeSyncRemoteDataSource()
        syncManager = SyncManager(database, fakeRemoteDataSource, cursorManager)
    }

    @After
    fun tearDown() = runBlocking {
        cursorManager.clearCursor()
        database.close()
    }

    @Test
    fun updateIncrementsServerVersion() = runBlocking {
        val remoteId = UUID.randomUUID().toString()
        val initialEntry = RemoteCollectionDto(
            remoteId = remoteId,
            containerCount = 10,
            timestamp = 1700000000000L,
            estimatedValueCents = 100L
        )

        // 1. Initial Insert
        val insertResult = fakeRemoteDataSource.pushCollection(initialEntry)
        assertTrue(insertResult is RemoteSyncResult.Success)
        val v1 = (insertResult as RemoteSyncResult.Success).remoteVersion
        assertTrue("Initial insert must receive positive version", v1 > 0L)

        // 2. Update to the same entity
        val updatedEntry = initialEntry.copy(containerCount = 25)
        val updateResult = fakeRemoteDataSource.pushCollection(updatedEntry)
        assertTrue(updateResult is RemoteSyncResult.Success)
        val v2 = (updateResult as RemoteSyncResult.Success).remoteVersion

        assertTrue("UPDATE must increment server_version (v2 > v1)", v2 > v1)
    }

    @Test
    fun successiveUpdatesProduceStrictlyMonotonicVersions() = runBlocking {
        val remoteId = UUID.randomUUID().toString()
        var currentEntry = RemoteSpotDto(
            remoteId = remoteId,
            name = "Spot Alpha",
            latitude = 38.71,
            longitude = -9.13,
            clientCreatedAt = 1700000000000L,
            clientUpdatedAt = 1700000000000L
        )

        val versions = mutableListOf<Long>()

        for (i in 1..5) {
            currentEntry = currentEntry.copy(name = "Spot Alpha v$i")
            val res = fakeRemoteDataSource.pushSpot(currentEntry)
            assertTrue(res is RemoteSyncResult.Success)
            versions.add((res as RemoteSyncResult.Success).remoteVersion)
        }

        assertEquals(5, versions.size)
        for (i in 0 until versions.size - 1) {
            assertTrue(
                "Version ${versions[i+1]} must be strictly greater than ${versions[i]}",
                versions[i+1] > versions[i]
            )
        }
    }

    @Test
    fun interleavedTableChangesAreNeverIgnoredByPull() = runBlocking {
        // Step 1: Create a Spot (assigned global version 1)
        val spotId = UUID.randomUUID().toString()
        fakeRemoteDataSource.pushSpot(
            RemoteSpotDto(
                remoteId = spotId,
                name = "Chiado Spot",
                latitude = 38.711,
                longitude = -9.141,
                clientCreatedAt = 1700000000000L,
                clientUpdatedAt = 1700000000000L
            )
        )

        // Step 2: Create a Collection associated with spot (assigned global version 2)
        val colId1 = UUID.randomUUID().toString()
        fakeRemoteDataSource.pushCollection(
            RemoteCollectionDto(
                remoteId = colId1,
                containerCount = 12,
                timestamp = 1700000010000L,
                estimatedValueCents = 120L,
                spotRemoteId = spotId
            )
        )

        // First Pull: Reconciles Spot (v1) and Collection (v2). Cursor advances to 2.
        val pull1 = syncManager.pullAndReconcile()
        assertTrue(pull1)
        assertEquals(2L, cursorManager.getCursor())
        assertEquals(1, database.spotDao().getAllSpots().first().size)
        assertEquals(1, database.collectionDao().getAllCollections().first().size)

        // Step 3: Interleaved mutation: update the Spot (assigned global version 3)
        fakeRemoteDataSource.pushSpot(
            RemoteSpotDto(
                remoteId = spotId,
                name = "Chiado Spot Updated",
                latitude = 38.711,
                longitude = -9.141,
                clientCreatedAt = 1700000000000L,
                clientUpdatedAt = 1700000020000L
            )
        )

        // Step 4: Create a Goal (assigned global version 4)
        val goalId = UUID.randomUUID().toString()
        fakeRemoteDataSource.pushGoal(
            RemoteGoalDto(
                remoteId = goalId,
                type = "MONTHLY",
                targetValue = 100,
                period = "MONTHLY",
                isActive = true,
                clientUpdatedAt = 1700000030000L
            )
        )

        // Second Pull: Starting from cursor 2. Must pull Spot (v3) and Goal (v4).
        val pull2 = syncManager.pullAndReconcile()
        assertTrue(pull2)
        assertEquals(4L, cursorManager.getCursor())

        val spots = database.spotDao().getAllSpots().first()
        assertEquals(1, spots.size)
        assertEquals("Chiado Spot Updated", spots.first().name)
        assertEquals(3L, spots.first().remoteVersion)

        val goals = database.goalDao().getAllGoals().first()
        assertEquals(1, goals.size)
        assertEquals(4L, goals.first().remoteVersion)

        // Collection is still intact
        assertEquals(1, database.collectionDao().getAllCollections().first().size)
    }

    @Test
    fun tombstonesReceiveNewServerVersionAndPropagateViaPull() = runBlocking {
        val colId = UUID.randomUUID().toString()
        // 1. Initial insert (version 1)
        fakeRemoteDataSource.pushCollection(
            RemoteCollectionDto(
                remoteId = colId,
                containerCount = 40,
                timestamp = 1700000000000L,
                estimatedValueCents = 400L
            )
        )

        // Pull initial state (cursor advances to 1)
        syncManager.pullAndReconcile()
        assertEquals(1L, cursorManager.getCursor())
        assertEquals(1, database.collectionDao().getAllCollections().first().size)

        // 2. Delete / Tombstone entity (must receive version 2 from global sequence)
        val deleteResult = fakeRemoteDataSource.deleteEntity(OutboxEntityType.COLLECTION_ENTRY.name, colId)
        assertTrue(deleteResult is RemoteSyncResult.Success)
        val deleteVersion = (deleteResult as RemoteSyncResult.Success).remoteVersion
        assertTrue("Delete must receive new version greater than 1", deleteVersion > 1L)

        // 3. Next pull with cursor=1 must retrieve the tombstone and delete local entity
        val pullSuccess = syncManager.pullAndReconcile()
        assertTrue(pullSuccess)
        assertEquals(deleteVersion, cursorManager.getCursor())

        val collectionsAfterTombstone = database.collectionDao().getAllCollections().first()
        assertTrue("Entity must be deleted locally following tombstone pull", collectionsAfterTombstone.isEmpty())
    }

    @Test
    fun sequenceInitializedAboveExistingDataNeverMovesBackwards() = runBlocking {
        // Migration algorithm simulation: target = maxOf(maxExisting, currentSeq, 1L)
        fun computeSequenceInit(maxExisting: Long, currentSeq: Long): Long {
            return maxOf(maxExisting, currentSeq, 1L)
        }

        // Case A: Table has existing version 150, sequence is at 1
        val initA = computeSequenceInit(maxExisting = 150L, currentSeq = 1L)
        assertEquals(150L, initA)

        // Case B: Table max is 50, but sequence has already progressed to 200 (never move backwards)
        val initB = computeSequenceInit(maxExisting = 50L, currentSeq = 200L)
        assertEquals(200L, initB)

        // Case C: Empty database, sequence at 1
        val initC = computeSequenceInit(maxExisting = 0L, currentSeq = 1L)
        assertEquals(1L, initC)

        // Apply initialized sequence value to remote data source and verify next mutation receives init + 1
        fakeRemoteDataSource.versionCounter.set(initA)
        val insertResult = fakeRemoteDataSource.pushCollection(
            RemoteCollectionDto(
                remoteId = "post-init-id",
                containerCount = 10,
                timestamp = System.currentTimeMillis(),
                estimatedValueCents = 100L
            )
        )
        val newVer = (insertResult as RemoteSyncResult.Success).remoteVersion
        assertTrue("Sequence initialization must yield version strictly greater than existing data", newVer > initA)
    }

    @Test
    fun exactlyOneNewVersionPerLogicalMutation() = runBlocking {
        fakeRemoteDataSource.versionCounter.set(100L)
        val id = UUID.randomUUID().toString()

        // 1. INSERT must yield a new strictly monotonic version
        val insertRes = fakeRemoteDataSource.pushCollection(
            RemoteCollectionDto(remoteId = id, containerCount = 5, timestamp = 1000L, estimatedValueCents = 50L)
        )
        val vInsert = (insertRes as RemoteSyncResult.Success).remoteVersion
        assertTrue("INSERT must yield version greater than initial counter", vInsert > 100L)

        // 2. UPDATE must yield a strictly monotonic version greater than INSERT
        val updateRes = fakeRemoteDataSource.pushCollection(
            RemoteCollectionDto(remoteId = id, containerCount = 15, timestamp = 2000L, estimatedValueCents = 150L)
        )
        val vUpdate = (updateRes as RemoteSyncResult.Success).remoteVersion
        assertTrue("UPDATE must yield version greater than previous version", vUpdate > vInsert)

        // 3. TOMBSTONE / DELETE must yield a strictly monotonic version greater than UPDATE
        val deleteRes = fakeRemoteDataSource.deleteEntity(OutboxEntityType.COLLECTION_ENTRY.name, id)
        val vDelete = (deleteRes as RemoteSyncResult.Success).remoteVersion
        assertTrue("TOMBSTONE must yield version greater than previous update version", vDelete > vUpdate)
    }

    @Test
    fun concurrentMutationDuringPullSnapshotIsNeverLost() = runBlocking {
        // Step 1: Server has 2 changes (v1, v2)
        fakeRemoteDataSource.pushSpot(
            RemoteSpotDto(remoteId = "spot-c1", name = "Spot 1", latitude = 38.0, longitude = -9.0, clientCreatedAt = 1L, clientUpdatedAt = 1L)
        )
        fakeRemoteDataSource.pushCollection(
            RemoteCollectionDto(remoteId = "col-c1", containerCount = 10, timestamp = 1L, estimatedValueCents = 100L)
        )

        // Step 2: Client pulls snapshot up to cursor=2
        val pull1 = syncManager.pullAndReconcile()
        assertTrue(pull1)
        assertEquals(2L, cursorManager.getCursor())

        // Step 3: Concurrent mutation occurs on server (v3) while client is idle
        fakeRemoteDataSource.pushCollection(
            RemoteCollectionDto(remoteId = "col-c2", containerCount = 20, timestamp = 2L, estimatedValueCents = 200L)
        )

        // Step 4: Next pull starts from cursor=2 and must retrieve v3
        val pull2 = syncManager.pullAndReconcile()
        assertTrue(pull2)
        assertEquals(3L, cursorManager.getCursor())

        val localCollections = database.collectionDao().getAllCollections().first()
        assertEquals(2, localCollections.size)
        assertTrue(localCollections.any { it.remoteId == "col-c2" })
    }

    @Test
    fun cursorOnlyAdvancesAfterEntirePayloadIsApplied() = runBlocking {
        cursorManager.setCursor(5L)

        // Simulate failing reconciler that crashes mid-transaction
        val failingReconciler = object : InboundSyncReconciler(database, cursorManager) {
            override suspend fun reconcile(response: RemoteSyncPullResponse, currentCursor: Long) {
                database.withTransaction {
                    // Throwing before commit
                    throw java.sql.SQLException("Simulated transaction failure during pull reconciliation")
                }
            }
        }

        val testSyncManager = SyncManager(
            database = database,
            remoteDataSource = fakeRemoteDataSource,
            cursorManager = cursorManager,
            reconciler = failingReconciler
        )

        // Pull changes with new server version 50
        fakeRemoteDataSource.setPullResponse(
            RemoteSyncPullResponse(
                collections = listOf(
                    RemoteCollectionDto(remoteId = "atomic-fail-id", containerCount = 10, timestamp = 1L, estimatedValueCents = 100L, serverVersion = 50L)
                ),
                newCursor = 50L
            )
        )

        val success = testSyncManager.pullAndReconcile()
        assertFalse("Pull must report failure when transaction fails", success)

        // Cursor MUST remain at 5, NOT 50
        assertEquals("Cursor must not advance if payload application failed", 5L, cursorManager.getCursor())
    }
}
