package com.example.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.CollectionEntryEntity
import com.example.data.local.SyncState
import com.example.repository.CollectionRepository
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

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SyncWorkerExecutionTest {

    private lateinit var database: AppDatabase
    private lateinit var collectionRepo: CollectionRepository
    private lateinit var fakeRemoteDataSource: FakeSyncRemoteDataSource
    private lateinit var syncManager: SyncManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        collectionRepo = CollectionRepository(database)
        fakeRemoteDataSource = FakeSyncRemoteDataSource()
        syncManager = SyncManager(database, fakeRemoteDataSource)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun workerSuccessRemovesOutboxAndMarksEntitySynced() = runBlocking {
        // 1. User records collection offline
        val entry = CollectionEntryEntity(
            containerCount = 20,
            timestamp = System.currentTimeMillis(),
            estimatedValueCents = 200L,
            note = "Chiado"
        )
        collectionRepo.insertCollection(entry)

        assertEquals(1, database.syncOutboxDao().observePendingOperations().first().size)
        assertEquals(SyncState.PENDING_UPLOAD.name, collectionRepo.getAllCollections().first().first().syncState)

        // 2. Sync runs
        val success = syncManager.processOutboxBatch()
        assertTrue("Sync batch must succeed", success)

        // 3. Outbox operation must be cleared
        val remainingOps = database.syncOutboxDao().observePendingOperations().first()
        assertEquals(0, remainingOps.size)

        // 4. Local entity must be updated to SYNCED with server metadata
        val syncedEntity = collectionRepo.getAllCollections().first().first()
        assertEquals(SyncState.SYNCED.name, syncedEntity.syncState)
        assertNotNull(syncedEntity.serverUpdatedAt)
        assertTrue(syncedEntity.remoteVersion > 0L)

        // 5. Remote data source received exactly 1 item
        assertEquals(1, fakeRemoteDataSource.pushedCollections.size)
        assertEquals(20, fakeRemoteDataSource.pushedCollections.first().containerCount)
    }

    @Test
    fun workerNetworkFailureKeepsOutboxPendingAndIncrementsRetryCount() = runBlocking {
        fakeRemoteDataSource.shouldFailWithNetworkError = true

        val entry = CollectionEntryEntity(
            containerCount = 5,
            timestamp = System.currentTimeMillis(),
            estimatedValueCents = 50L
        )
        collectionRepo.insertCollection(entry)

        // Run sync with simulated network error
        val success = syncManager.processOutboxBatch()
        assertFalse("Sync batch must report failure", success)

        // Operation remains in outbox for retry
        val pendingOps = database.syncOutboxDao().observePendingOperations().first()
        assertEquals(1, pendingOps.size)
        val op = pendingOps.first()
        assertEquals(1, op.retryCount)
        assertNotNull(op.lastAttemptAt)
        assertEquals("Simulated network timeout", op.lastError)
    }
}
