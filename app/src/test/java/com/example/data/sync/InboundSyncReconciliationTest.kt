package com.example.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.CollectionEntryEntity
import com.example.data.local.SyncState
import com.example.repository.CollectionRepository
import androidx.room.withTransaction
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
class InboundSyncReconciliationTest {

    private lateinit var database: AppDatabase
    private lateinit var cursorManager: SyncCursorManager
    private lateinit var fakeRemoteDataSource: FakeSyncRemoteDataSource
    private lateinit var syncManager: SyncManager
    private lateinit var collectionRepo: CollectionRepository

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
        collectionRepo = CollectionRepository(database)
    }

    @After
    fun tearDown() = runBlocking {
        cursorManager.clearCursor()
        database.close()
    }

    @Test
    fun inboundInsertPersistsNewEntityWithSyncedState() = runBlocking {
        val remoteId = UUID.randomUUID().toString()
        val pullResponse = RemoteSyncPullResponse(
            collections = listOf(
                RemoteCollectionDto(
                    remoteId = remoteId,
                    containerCount = 15,
                    timestamp = 1700000000000L,
                    estimatedValueCents = 150L,
                    note = "Lisbon Center",
                    serverUpdatedAt = "2026-09-16T12:00:00Z",
                    serverVersion = 42L
                )
            ),
            newCursor = 42L
        )
        fakeRemoteDataSource.setPullResponse(pullResponse)

        val success = syncManager.pullAndReconcile()
        assertTrue(success)

        val collections = database.collectionDao().getAllCollections().first()
        assertEquals(1, collections.size)
        val saved = collections.first()
        assertEquals(remoteId, saved.remoteId)
        assertEquals(15, saved.containerCount)
        assertEquals(SyncState.SYNCED.name, saved.syncState)
        assertEquals(42L, saved.remoteVersion)
        assertEquals(42L, cursorManager.getCursor())
    }

    @Test
    fun pendingOutboxProtectsLocalUnsyncedEditFromBeingOverwritten() = runBlocking {
        val remoteId = UUID.randomUUID().toString()
        // 1. User records a collection locally (pending upload in outbox)
        val localEntry = CollectionEntryEntity(
            remoteId = remoteId,
            containerCount = 50,
            timestamp = System.currentTimeMillis(),
            estimatedValueCents = 500L,
            note = "Local Offline Edit"
        )
        collectionRepo.insertCollection(localEntry)

        // 2. Server returns a conflicting / older record for the same remoteId
        val pullResponse = RemoteSyncPullResponse(
            collections = listOf(
                RemoteCollectionDto(
                    remoteId = remoteId,
                    containerCount = 5,
                    timestamp = 1600000000000L,
                    estimatedValueCents = 50L,
                    note = "Server Stale Version",
                    serverVersion = 10L
                )
            ),
            newCursor = 10L
        )
        fakeRemoteDataSource.setPullResponse(pullResponse)

        // 3. Pull & Reconcile must protect local pending outbox change
        val success = syncManager.pullAndReconcile()
        assertTrue(success)

        val current = database.collectionDao().getAllCollections().first().first()
        assertEquals(50, current.containerCount)
        assertEquals("Local Offline Edit", current.note)
        assertEquals(SyncState.PENDING_UPLOAD.name, current.syncState)
    }

    @Test
    fun remoteTombstoneDeletesLocalEntityWhenNoPendingOutbox() = runBlocking {
        val remoteId = UUID.randomUUID().toString()
        // Insert an already-synced entity
        val syncedEntry = CollectionEntryEntity(
            remoteId = remoteId,
            containerCount = 10,
            timestamp = 1700000000000L,
            estimatedValueCents = 100L,
            syncState = SyncState.SYNCED.name,
            remoteVersion = 5L
        )
        database.collectionDao().insertCollection(syncedEntry)

        // Server sends tombstone (deletedAt is non-null)
        val pullResponse = RemoteSyncPullResponse(
            collections = listOf(
                RemoteCollectionDto(
                    remoteId = remoteId,
                    containerCount = 10,
                    timestamp = 1700000000000L,
                    estimatedValueCents = 100L,
                    deletedAt = "2026-09-16T13:00:00Z",
                    serverVersion = 6L
                )
            ),
            newCursor = 6L
        )
        fakeRemoteDataSource.setPullResponse(pullResponse)

        val success = syncManager.pullAndReconcile()
        assertTrue(success)

        val collections = database.collectionDao().getAllCollections().first()
        assertTrue(collections.isEmpty())
        assertEquals(6L, cursorManager.getCursor())
    }

    @Test
    fun replayIdempotencyMaintainsIdenticalStateWithoutDuplicates() = runBlocking {
        val remoteId = UUID.randomUUID().toString()
        val pullResponse = RemoteSyncPullResponse(
            collections = listOf(
                RemoteCollectionDto(
                    remoteId = remoteId,
                    containerCount = 25,
                    timestamp = 1700000000000L,
                    estimatedValueCents = 250L,
                    note = "Lisbon Replay Test",
                    serverVersion = 50L
                )
            ),
            newCursor = 50L
        )
        fakeRemoteDataSource.setPullResponse(pullResponse)

        // 1st apply
        val firstRun = syncManager.pullAndReconcile()
        assertTrue(firstRun)
        assertEquals(1, database.collectionDao().getAllCollections().first().size)
        assertEquals(50L, cursorManager.getCursor())

        // 2nd apply (exact same replay)
        val secondRun = syncManager.pullAndReconcile()
        assertTrue(secondRun)
        val collectionsAfterReplay = database.collectionDao().getAllCollections().first()
        assertEquals(1, collectionsAfterReplay.size) // No duplicates
        val entry = collectionsAfterReplay.first()
        assertEquals(25, entry.containerCount)
        assertEquals(SyncState.SYNCED.name, entry.syncState)
        assertEquals(50L, cursorManager.getCursor())
    }

    @Test
    fun crashConsistencyRollsBackBothDataAndCursorOnFailure() = runBlocking {
        // Prepare initial cursor state
        cursorManager.setCursor(10L)

        // Failing reconciler simulating a mid-transaction SQLite crash
        val failingReconciler = object : InboundSyncReconciler(database, cursorManager) {
            override suspend fun reconcile(response: RemoteSyncPullResponse, currentCursor: Long) {
                database.withTransaction {
                    // Step 1: Insert an entry
                    database.collectionDao().insertCollection(
                        CollectionEntryEntity(
                            remoteId = "crash-test-id",
                            containerCount = 99,
                            timestamp = 1700000000000L,
                            estimatedValueCents = 990L
                        )
                    )
                    // Step 2: Simulate crash before commit
                    throw java.sql.SQLException("Simulated power failure / disk crash")
                }
            }
        }

        val testSyncManager = SyncManager(
            database = database,
            remoteDataSource = fakeRemoteDataSource,
            cursorManager = cursorManager,
            reconciler = failingReconciler
        )

        val success = testSyncManager.pullAndReconcile()
        assertFalse("Pull must fail when transaction throws exception", success)

        // Verify ACID rollback: neither the collection nor the cursor changed
        val collections = database.collectionDao().getAllCollections().first()
        assertTrue("Rolled-back collection must not exist", collections.none { it.remoteId == "crash-test-id" })
        assertEquals("Sync cursor must not advance on crash", 10L, cursorManager.getCursor())
    }

    @Test
    fun offlineBehaviorPreservesOutboxAndDoesNotCorruptLocalState() = runBlocking {
        // User inserts offline entry
        val entry = CollectionEntryEntity(
            containerCount = 30,
            timestamp = System.currentTimeMillis(),
            estimatedValueCents = 300L,
            note = "Offline Pending"
        )
        collectionRepo.insertCollection(entry)
        assertEquals(1, database.syncOutboxDao().observePendingOperations().first().size)

        // Simulate network failure
        fakeRemoteDataSource.shouldFailWithNetworkError = true

        val syncSuccess = syncManager.syncAll()
        assertFalse(syncSuccess)

        // Outbox must remain intact with retryable state
        assertEquals(1, database.syncOutboxDao().observePendingOperations().first().size)
        val current = database.collectionDao().getAllCollections().first().first()
        assertEquals(SyncState.PENDING_UPLOAD.name, current.syncState)
        assertEquals(0L, cursorManager.getCursor())
    }

    @Test
    fun multiBatchOutboxDrainingTest() = runBlocking {
        // Enqueue 110 items across 3 batches (50 + 50 + 10)
        val totalItems = 110
        for (i in 1..totalItems) {
            val entry = CollectionEntryEntity(
                remoteId = "batch-item-$i",
                containerCount = i,
                timestamp = System.currentTimeMillis() + i,
                estimatedValueCents = i * 10L
            )
            collectionRepo.insertCollection(entry)
        }

        assertEquals(totalItems, database.syncOutboxDao().observePendingOperations().first().size)

        // syncAll must loop through batches until outbox is completely drained
        val syncSuccess = syncManager.syncAll()
        assertTrue("syncAll must succeed draining multiple batches", syncSuccess)

        // Verify outbox is completely empty
        val remainingPending = database.syncOutboxDao().observePendingOperations().first()
        assertEquals("Outbox must be fully drained", 0, remainingPending.size)

        // Verify all 110 items in local Room are marked as SYNCED
        val allCollections = database.collectionDao().getAllCollections().first()
        assertEquals(totalItems, allCollections.size)
        assertTrue("All entries must be marked SYNCED", allCollections.all { it.syncState == SyncState.SYNCED.name })
    }

    @Test
    fun partialNetworkInterruptionPreservesOrderAndHaltsBatch() = runBlocking {
        val entry1 = CollectionEntryEntity(remoteId = "item-1", containerCount = 10, timestamp = 1000L, estimatedValueCents = 100L)
        val entry2 = CollectionEntryEntity(remoteId = "item-2", containerCount = 20, timestamp = 2000L, estimatedValueCents = 200L)
        val entry3 = CollectionEntryEntity(remoteId = "item-3", containerCount = 30, timestamp = 3000L, estimatedValueCents = 300L)

        collectionRepo.insertCollection(entry1)
        collectionRepo.insertCollection(entry2)
        collectionRepo.insertCollection(entry3)

        assertEquals(3, database.syncOutboxDao().observePendingOperations().first().size)

        // Set failure hook specifically for item-2
        fakeRemoteDataSource.failingRemoteIds = setOf("item-2")

        val batchSuccess = syncManager.processOutboxBatch(50)
        assertFalse("Batch must return false on network error", batchSuccess)

        // Item 1 succeeded and was removed from outbox
        val pending = database.syncOutboxDao().observePendingOperations().first()
        assertEquals(2, pending.size)
        assertEquals("item-2", pending[0].entityRemoteId)
        assertEquals(1, pending[0].retryCount)
        assertEquals("item-3", pending[1].entityRemoteId)
        assertEquals(0, pending[1].retryCount) // Untouched to preserve strict chronological ordering
    }
}
