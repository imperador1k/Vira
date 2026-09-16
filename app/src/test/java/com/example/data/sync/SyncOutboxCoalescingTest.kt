package com.example.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.OutboxEntityType
import com.example.data.local.OutboxOperationType
import com.example.data.local.SyncOutboxDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SyncOutboxCoalescingTest {

    private lateinit var database: AppDatabase
    private lateinit var outboxDao: SyncOutboxDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        outboxDao = database.syncOutboxDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun multipleUpsertsOnSameEntityCoalesceToSingleLatestOperation() = runBlocking {
        val remoteId = UUID.randomUUID().toString()

        // 1st edit
        outboxDao.enqueueCoalesced(
            entityType = OutboxEntityType.COLLECTION_ENTRY.name,
            entityRemoteId = remoteId,
            operationType = OutboxOperationType.UPSERT.name,
            payloadJson = "{\"count\": 5}",
            isLocallyCreatedOnly = true
        )

        // 2nd edit
        outboxDao.enqueueCoalesced(
            entityType = OutboxEntityType.COLLECTION_ENTRY.name,
            entityRemoteId = remoteId,
            operationType = OutboxOperationType.UPSERT.name,
            payloadJson = "{\"count\": 10}",
            isLocallyCreatedOnly = true
        )

        // 3rd edit
        outboxDao.enqueueCoalesced(
            entityType = OutboxEntityType.COLLECTION_ENTRY.name,
            entityRemoteId = remoteId,
            operationType = OutboxOperationType.UPSERT.name,
            payloadJson = "{\"count\": 25}",
            isLocallyCreatedOnly = true
        )

        val pending = outboxDao.observePendingOperations().first()
        assertEquals("Multiple edits must coalesce to exactly 1 operation", 1, pending.size)
        val op = pending.first()
        assertEquals(remoteId, op.entityRemoteId)
        assertEquals(OutboxOperationType.UPSERT.name, op.operationType)
        assertEquals("{\"count\": 25}", op.payloadJson)
    }

    @Test
    fun localOnlyEntityCreatedAndDeletedBeforeSyncCoalescesToNoOp() = runBlocking {
        val remoteId = UUID.randomUUID().toString()

        // Created locally (isLocallyCreatedOnly = true, remoteVersion = 0)
        outboxDao.enqueueCoalesced(
            entityType = OutboxEntityType.COLLECTION_ENTRY.name,
            entityRemoteId = remoteId,
            operationType = OutboxOperationType.UPSERT.name,
            payloadJson = "{\"count\": 10}",
            isLocallyCreatedOnly = true
        )

        assertEquals(1, outboxDao.observePendingOperations().first().size)

        // User deletes it before remote sync
        outboxDao.enqueueCoalesced(
            entityType = OutboxEntityType.COLLECTION_ENTRY.name,
            entityRemoteId = remoteId,
            operationType = OutboxOperationType.DELETE.name,
            isLocallyCreatedOnly = true
        )

        val pending = outboxDao.observePendingOperations().first()
        assertEquals("Local-only creation + deletion before sync must collapse to 0 remote operations", 0, pending.size)
    }

    @Test
    fun previouslySyncedEntityEditedAndDeletedCoalescesToSingleDelete() = runBlocking {
        val remoteId = UUID.randomUUID().toString()

        // Entity previously existed on remote (remoteVersion > 0)
        // User edits it locally
        outboxDao.enqueueCoalesced(
            entityType = OutboxEntityType.COLLECTION_ENTRY.name,
            entityRemoteId = remoteId,
            operationType = OutboxOperationType.UPSERT.name,
            payloadJson = "{\"count\": 30}",
            isLocallyCreatedOnly = false
        )

        // User then deletes it
        outboxDao.enqueueCoalesced(
            entityType = OutboxEntityType.COLLECTION_ENTRY.name,
            entityRemoteId = remoteId,
            operationType = OutboxOperationType.DELETE.name,
            isLocallyCreatedOnly = false
        )

        val pending = outboxDao.observePendingOperations().first()
        assertEquals(1, pending.size)
        val op = pending.first()
        assertEquals(remoteId, op.entityRemoteId)
        assertEquals(OutboxOperationType.DELETE.name, op.operationType)
        assertNull("Delete operation payload must be null", op.payloadJson)
    }
}
