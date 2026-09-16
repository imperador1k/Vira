package com.example.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.CollectionEntryEntity
import com.example.data.local.CollectionSpotEntity
import com.example.data.local.OutboxEntityType
import com.example.data.local.OutboxOperationType
import com.example.data.local.SyncState
import com.example.repository.CollectionRepository
import com.example.repository.SpotRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LocalWriteTransactionTest {

    private lateinit var context: Context
    private val dbName = "local_write_test.db"
    private lateinit var database: AppDatabase
    private lateinit var collectionRepo: CollectionRepository
    private lateinit var spotRepo: SpotRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
        database = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        collectionRepo = CollectionRepository(database)
        spotRepo = SpotRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun localWriteCreatesEntityAndOutboxItemAtomically() = runBlocking {
        val entry = CollectionEntryEntity(
            containerCount = 12,
            timestamp = System.currentTimeMillis(),
            estimatedValueCents = 120L,
            note = "Praça do Comércio"
        )

        val id = collectionRepo.insertCollection(entry)
        assertTrue(id > 0)

        // 1. Verify entity saved with PENDING_UPLOAD
        val collections = collectionRepo.getAllCollections().first()
        assertEquals(1, collections.size)
        val saved = collections.first()
        assertEquals(12, saved.containerCount)
        assertEquals(SyncState.PENDING_UPLOAD.name, saved.syncState)
        assertNotNull(saved.remoteId)

        // 2. Verify outbox entry created
        val pendingOps = database.syncOutboxDao().observePendingOperations().first()
        assertEquals(1, pendingOps.size)
        val op = pendingOps.first()
        assertEquals(OutboxEntityType.COLLECTION_ENTRY.name, op.entityType)
        assertEquals(saved.remoteId, op.entityRemoteId)
        assertEquals(OutboxOperationType.UPSERT.name, op.operationType)
    }

    @Test
    fun deleteSetsTombstoneAndHandlesOutboxCorrectly() = runBlocking {
        // Insert collection
        val entry = CollectionEntryEntity(
            containerCount = 15,
            timestamp = System.currentTimeMillis(),
            estimatedValueCents = 150L
        )
        val id = collectionRepo.insertCollection(entry).toInt()
        val saved = collectionRepo.getAllCollections().first().first()

        // Delete collection (was local only)
        collectionRepo.deleteCollectionById(id)

        // UI observation immediately hides deleted entry
        val activeCollections = collectionRepo.getAllCollections().first()
        assertEquals(0, activeCollections.size)

        // Since it was local-only (never synced to remote), pending outbox op is cancelled (no-op sync)
        val pendingOps = database.syncOutboxDao().observePendingOperations().first()
        assertEquals(0, pendingOps.size)
    }

    @Test
    fun appRestartPreservesPendingSyncOperations() = runBlocking {
        // Insert spot
        val spot = CollectionSpotEntity(
            name = "Parque das Nações",
            latitude = 38.767,
            longitude = -9.096
        )
        spotRepo.insertSpot(spot)

        // Verify outbox has 1 operation
        assertEquals(1, database.syncOutboxDao().observePendingOperations().first().size)

        // Simulate app kill & restart
        database.close()

        val reopenedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()

        val preservedOps = reopenedDb.syncOutboxDao().observePendingOperations().first()
        assertEquals(1, preservedOps.size)
        assertEquals(OutboxEntityType.COLLECTION_SPOT.name, preservedOps.first().entityType)

        reopenedDb.close()
    }
}
