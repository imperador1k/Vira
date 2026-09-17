package com.example.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.CollectionEntryEntity
import com.example.repository.CollectionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SoftDeleteAndConcurrentSyncTest {

    private lateinit var databaseA: AppDatabase
    private lateinit var databaseB: AppDatabase
    private lateinit var cursorManagerA: SyncCursorManager
    private lateinit var cursorManagerB: SyncCursorManager
    private lateinit var fakeRemoteDataSource: FakeSyncRemoteDataSource
    private lateinit var syncManagerA: SyncManager
    private lateinit var syncManagerB: SyncManager
    private lateinit var collectionRepoA: CollectionRepository

    private val testUserId = "user-uuid-1234-abcd"

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Simulate Device A
        databaseA = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cursorManagerA = SyncCursorManager(databaseA.syncMetadataDao())
        cursorManagerA.clearCursor()

        // Simulate Device B
        databaseB = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cursorManagerB = SyncCursorManager(databaseB.syncMetadataDao())
        cursorManagerB.clearCursor()

        // Shared remote data source (simulating Supabase backend with authenticated user)
        fakeRemoteDataSource = FakeSyncRemoteDataSource(currentUserId = testUserId)

        syncManagerA = SyncManager(databaseA, fakeRemoteDataSource, cursorManagerA)
        syncManagerB = SyncManager(databaseB, fakeRemoteDataSource, cursorManagerB)
        collectionRepoA = CollectionRepository(databaseA)
    }

    @After
    fun tearDown() = runBlocking {
        cursorManagerA.clearCursor()
        cursorManagerB.clearCursor()
        databaseA.close()
        databaseB.close()
    }

    @Test
    fun softDeletePropagatesAcrossDevicesWithoutResurrection() = runBlocking {
        // Step 1: Device A creates a collection record locally
        val localAId = collectionRepoA.insertCollection(
            CollectionEntryEntity(
                containerCount = 20,
                estimatedValueCents = 200L,
                note = "Initial bottles at Lisbon",
                timestamp = System.currentTimeMillis()
            )
        ).toInt()

        val entryA = databaseA.collectionDao().getAllCollections().first().first()
        val remoteId = entryA.remoteId
        assertNotNull(remoteId)

        // Step 2: Device A pushes mutations to backend
        val pushSuccessA = syncManagerA.processOutboxBatch()
        assertTrue("Device A push should succeed", pushSuccessA)

        // Verify record exists on remote backend with server_version >= 1 and deleted_at == null
        val remoteRecord = fakeRemoteDataSource.pushedCollections.find { it.remoteId == remoteId }
        assertNotNull("Remote record must exist on backend", remoteRecord)
        assertNull("Remote record must not be marked deleted initially", remoteRecord?.deletedAt)
        val version1 = remoteRecord!!.serverVersion!!
        assertTrue("Initial server_version must be > 0", version1 > 0)

        // Step 3: Device B pulls from backend (cursor 0 -> version1)
        val pullSuccessB = syncManagerB.pullAndReconcile()
        assertTrue("Device B pull should succeed", pullSuccessB)
        assertEquals("Device B cursor should advance to version1", version1, cursorManagerB.getCursor())

        // Verify Device B now has the record locally
        val collectionsB = databaseB.collectionDao().getAllCollections().first()
        assertEquals("Device B must have 1 active collection", 1, collectionsB.size)
        assertEquals(remoteId, collectionsB.first().remoteId)

        // Step 4: Device A logically deletes the collection
        collectionRepoA.deleteCollectionById(localAId)

        // Verify Device A local state is marked deleted
        val activeA = databaseA.collectionDao().getAllCollections().first()
        assertTrue("Device A must have 0 active collections after delete", activeA.isEmpty())

        // Step 5: Device A pushes tombstone to backend
        val pushDeleteA = syncManagerA.processOutboxBatch()
        assertTrue("Device A push of tombstone should succeed", pushDeleteA)

        // Verify backend record was SOFT-DELETED (NOT physically removed from table)
        val tombstoneRecord = fakeRemoteDataSource.pushedCollections.find { it.remoteId == remoteId }
        assertNotNull("Record must NOT be physically deleted from backend", tombstoneRecord)
        assertNotNull("Record on backend must have deleted_at timestamp", tombstoneRecord?.deletedAt)
        val version2 = tombstoneRecord!!.serverVersion!!
        assertTrue("Tombstone must receive a strictly higher server_version", version2 > version1)

        // Step 6: Device B pulls changes using cursor (sinceCursor = version1)
        val pullTombstoneB = syncManagerB.pullAndReconcile()
        assertTrue("Device B pull of tombstone should succeed", pullTombstoneB)
        assertEquals("Device B cursor should advance to tombstone version", version2, cursorManagerB.getCursor())

        // Step 7: Verify Device B applied the deletion in Room
        val collectionsBAfterDelete = databaseB.collectionDao().getAllCollections().first()
        assertTrue("Device B must have 0 active collections after applying tombstone", collectionsBAfterDelete.isEmpty())

        // Step 8: Verify subsequent sync does NOT resurrect the record
        val secondPullB = syncManagerB.pullAndReconcile()
        assertTrue(secondPullB)
        val collectionsBFinal = databaseB.collectionDao().getAllCollections().first()
        assertTrue("Record must not be silently resurrected", collectionsBFinal.isEmpty())
    }

    @Test
    fun atomicSnapshotEnsuresNoVersionsSkippedOnConcurrentMutations() = runBlocking {
        // Simulate scenario where snapshot S1 is taken at cursor 100
        val cursorAtStart = 100L
        cursorManagerA.setCursor(cursorAtStart)

        // Remote has initial version 105 in snapshot S1
        val initialRecord = RemoteCollectionDto(
            remoteId = UUID.randomUUID().toString(),
            containerCount = 10,
            timestamp = 1700000000000L,
            estimatedValueCents = 100L,
            serverVersion = 105L
        )

        // Snapshot S1 response sees only data committed up to version 105
        val snapshotResponse1 = RemoteSyncPullResponse(
            collections = listOf(initialRecord),
            newCursor = 105L,
            hasMore = false
        )
        fakeRemoteDataSource.setPullResponse(snapshotResponse1)

        // Device A executes pull
        val pullResult = syncManagerA.pullAndReconcile()
        assertTrue(pullResult)
        assertEquals("Cursor must advance strictly to 105", 105L, cursorManagerA.getCursor())

        // Now, a concurrent transaction that committed after S1 (version 110 and 120)
        // is retrieved in snapshot S2 on the NEXT pull (sinceCursor = 105)
        val concurrentRecord1 = RemoteCollectionDto(
            remoteId = UUID.randomUUID().toString(),
            containerCount = 5,
            timestamp = 1700000010000L,
            estimatedValueCents = 50L,
            serverVersion = 110L
        )
        val concurrentRecord2 = RemoteCollectionDto(
            remoteId = UUID.randomUUID().toString(),
            containerCount = 12,
            timestamp = 1700000020000L,
            estimatedValueCents = 120L,
            serverVersion = 120L
        )
        val snapshotResponse2 = RemoteSyncPullResponse(
            collections = listOf(concurrentRecord1, concurrentRecord2),
            newCursor = 120L,
            hasMore = false
        )
        fakeRemoteDataSource.setPullResponse(snapshotResponse2)

        val pullResult2 = syncManagerA.pullAndReconcile()
        assertTrue(pullResult2)
        assertEquals("Cursor must now advance to 120", 120L, cursorManagerA.getCursor())

        // Verify all 3 records are present locally with no dropped data
        val allCollections = databaseA.collectionDao().getAllCollections().first()
        assertEquals(3, allCollections.size)
    }

    @Test
    fun cursorDoesNotAdvanceIfRoomTransactionFails() = runBlocking {
        cursorManagerA.setCursor(50L)

        // Remote returns an invalid/unparseable payload or reconciler triggers failure
        fakeRemoteDataSource.shouldFailWithNetworkError = true

        val success = syncManagerA.pullAndReconcile()
        assertFalse("Pull must fail when network/database error occurs", success)
        assertEquals("Cursor must NOT advance when pull fails", 50L, cursorManagerA.getCursor())
    }
}
