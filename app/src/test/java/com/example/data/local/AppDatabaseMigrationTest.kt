package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class AppDatabaseMigrationTest {

    private lateinit var context: Context
    private val dbName = "migration_test.db"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    private fun createOpenHelper(version: Int): SupportSQLiteDatabase {
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create v1 tables
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS user_profile (
                            id INTEGER PRIMARY KEY NOT NULL,
                            name TEXT NOT NULL,
                            memberSince INTEGER NOT NULL,
                            monthlyGoalId INTEGER,
                            themePreference TEXT NOT NULL
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS collection_entry (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            containerCount INTEGER NOT NULL,
                            timestamp INTEGER NOT NULL,
                            estimatedValueCents INTEGER NOT NULL,
                            collectionSpotId INTEGER,
                            note TEXT,
                            latitude REAL,
                            longitude REAL
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS collection_spot (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL,
                            latitude REAL NOT NULL,
                            longitude REAL NOT NULL,
                            address TEXT,
                            createdAt INTEGER NOT NULL,
                            lastVisitedAt INTEGER,
                            totalVisits INTEGER NOT NULL,
                            lifetimeContainers INTEGER NOT NULL,
                            averageContainersPerVisit REAL NOT NULL
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS redemption_entry (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            presentedContainers INTEGER NOT NULL,
                            acceptedContainers INTEGER NOT NULL,
                            rejectedContainers INTEGER NOT NULL,
                            actualRecoveredCents INTEGER NOT NULL,
                            timestamp INTEGER NOT NULL,
                            returnPointId INTEGER,
                            note TEXT
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS return_point (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL,
                            type TEXT NOT NULL,
                            latitude REAL NOT NULL,
                            longitude REAL NOT NULL,
                            address TEXT,
                            source TEXT NOT NULL,
                            isVerified INTEGER NOT NULL,
                            lastUpdatedTimestamp INTEGER NOT NULL,
                            userNotes TEXT
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS goal (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            type TEXT NOT NULL,
                            targetValue INTEGER NOT NULL,
                            period TEXT NOT NULL,
                            isActive INTEGER NOT NULL
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
    }

    @Test
    fun migration_from_v1_to_v2_preserves_data_and_transforms_return_points() {
        val db = createOpenHelper(1)

        db.execSQL("INSERT INTO user_profile (id, name, memberSince, monthlyGoalId, themePreference) VALUES (1, 'Vitor', 1690000000000, 1, 'SYSTEM')")
        db.execSQL("INSERT INTO collection_entry (id, containerCount, timestamp, estimatedValueCents, collectionSpotId, note, latitude, longitude) VALUES (1, 24, 1690001000000, 240, 1, 'Praia de Carcavelos', 38.68, -9.33)")
        db.execSQL("INSERT INTO collection_spot (id, name, latitude, longitude, address, createdAt, lastVisitedAt, totalVisits, lifetimeContainers, averageContainersPerVisit) VALUES (1, 'Carcavelos Spot', 38.68, -9.33, 'Praia', 1690000000000, 1690001000000, 1, 24, 24.0)")
        db.execSQL("INSERT INTO redemption_entry (id, presentedContainers, acceptedContainers, rejectedContainers, actualRecoveredCents, timestamp, returnPointId, note) VALUES (1, 24, 24, 0, 240, 1690002000000, 1, 'Tudo ok')")
        db.execSQL("INSERT INTO return_point (id, name, type, latitude, longitude, address, source, isVerified, lastUpdatedTimestamp, userNotes) VALUES (1, 'Pingo Doce', 'AUTOMATIC_MACHINE', 38.71, -9.13, 'Rua 1', 'OFFICIAL_SDR', 1, 1690000500000, null)")

        AppDatabase.MIGRATION_1_2.migrate(db)

        val colCursor = db.query("SELECT containerCount, estimatedValueCents, timestamp FROM collection_entry WHERE id = 1")
        assertTrue(colCursor.moveToFirst())
        assertEquals(24, colCursor.getInt(0))
        assertEquals(240L, colCursor.getLong(1))
        assertEquals(1690001000000L, colCursor.getLong(2))
        colCursor.close()

        val rpCursor = db.query("SELECT name, verificationStatus, lastVerifiedAt, createdAt, updatedAt FROM return_point WHERE id = 1")
        assertTrue(rpCursor.moveToFirst())
        assertEquals("Pingo Doce", rpCursor.getString(0))
        assertEquals("VERIFIED", rpCursor.getString(1))
        assertEquals(1690000500000L, rpCursor.getLong(2))
        assertEquals(1690000500000L, rpCursor.getLong(3))
        assertEquals(1690000500000L, rpCursor.getLong(4))
        rpCursor.close()

        db.close()
    }

    @Test
    fun migration_from_v2_to_v3_preserves_collections_and_adds_sync_metadata() {
        val db = createOpenHelper(1)
        AppDatabase.MIGRATION_1_2.migrate(db)

        db.execSQL("INSERT INTO collection_entry (id, containerCount, timestamp, estimatedValueCents, collectionSpotId, note, latitude, longitude) VALUES (10, 50, 1695000000000, 500, null, 'Festival', 38.70, -9.14)")
        db.execSQL("INSERT INTO return_point (id, name, latitude, longitude, type, source, verificationStatus, lastVerifiedAt, address, openingHours, createdAt, updatedAt) VALUES (2, 'Loja Manual', 38.72, -9.15, 'MANUAL_STORE', 'COMMUNITY', 'COMMUNITY_SUBMITTED', null, 'Rua Nova', '09:00-19:00', 1695000000000, 1695000000000)")
        db.execSQL("INSERT INTO goal (id, type, targetValue, period, isActive) VALUES (1, 'CONTAINERS', 100, 'MONTHLY', 1)")

        AppDatabase.MIGRATION_2_3.migrate(db)

        val colCursor = db.query("SELECT containerCount, timestamp, remoteId, createdAt, updatedAt, deletedAt, syncState FROM collection_entry WHERE id = 10")
        assertTrue(colCursor.moveToFirst())
        assertEquals(50, colCursor.getInt(0))
        assertEquals(1695000000000L, colCursor.getLong(1))
        assertNull(colCursor.getString(2))
        assertEquals(1695000000000L, colCursor.getLong(3))
        assertEquals(1695000000000L, colCursor.getLong(4))
        assertTrue(colCursor.isNull(5))
        assertEquals("LOCAL_ONLY", colCursor.getString(6))
        colCursor.close()

        val rpCursor = db.query("SELECT type, source, verificationStatus, syncState FROM return_point WHERE id = 2")
        assertTrue(rpCursor.moveToFirst())
        assertEquals("MANUAL_POINT", rpCursor.getString(0))
        assertEquals("COMMUNITY", rpCursor.getString(1))
        assertEquals("COMMUNITY_CONFIRMED", rpCursor.getString(2))
        assertEquals("LOCAL_ONLY", rpCursor.getString(3))
        rpCursor.close()

        db.close()
    }

    @Test
    fun migration_from_v3_to_v4_creates_outbox_and_populates_stable_uuids() {
        val db = createOpenHelper(1)
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)

        // Seed v3 row with null remoteId
        db.execSQL("INSERT INTO collection_entry (id, containerCount, timestamp, estimatedValueCents, createdAt, updatedAt, remoteId, syncState) VALUES (15, 30, 1700000000000, 300, 1700000000000, 1700000000000, null, 'LOCAL_ONLY')")

        // Apply MIGRATION_3_4
        AppDatabase.MIGRATION_3_4.migrate(db)

        // 1. Verify remoteId was populated with non-null valid UUID
        val cursor = db.query("SELECT remoteId, serverUpdatedAt, remoteVersion FROM collection_entry WHERE id = 15")
        assertTrue(cursor.moveToFirst())
        val generatedRemoteId = cursor.getString(0)
        assertNotNull(generatedRemoteId)
        assertTrue(generatedRemoteId.isNotEmpty())
        assertNull(cursor.getString(1)) // serverUpdatedAt starts null
        assertEquals(0L, cursor.getLong(2)) // remoteVersion starts at 0
        cursor.close()

        // 2. Verify sync_outbox exists and can accept inserts
        db.execSQL("INSERT INTO sync_outbox (operationId, entityType, entityRemoteId, operationType, createdAt, retryCount, payloadVersion) VALUES ('op-1', 'COLLECTION_ENTRY', '$generatedRemoteId', 'UPSERT', 1700000000000, 0, 1)")
        val outboxCursor = db.query("SELECT operationId, entityRemoteId FROM sync_outbox WHERE operationId = 'op-1'")
        assertTrue(outboxCursor.moveToFirst())
        assertEquals("op-1", outboxCursor.getString(0))
        assertEquals(generatedRemoteId, outboxCursor.getString(1))
        outboxCursor.close()

        db.close()
    }

    @Test
    fun full_migration_chain_opens_in_room_and_survives_without_data_loss() = runBlocking {
        val rawDb = createOpenHelper(1)
        rawDb.execSQL("INSERT INTO user_profile (id, name, memberSince, monthlyGoalId, themePreference) VALUES (1, 'Maria', 1680000000000, null, 'SYSTEM')")
        rawDb.execSQL("INSERT INTO collection_spot (id, name, latitude, longitude, address, createdAt, lastVisitedAt, totalVisits, lifetimeContainers, averageContainersPerVisit) VALUES (1, 'Miradouro', 38.712, -9.130, null, 1680000000000, 1680005000000, 2, 35, 17.5)")
        rawDb.execSQL("INSERT INTO collection_entry (id, containerCount, timestamp, estimatedValueCents, collectionSpotId, note, latitude, longitude) VALUES (1, 15, 1680001000000, 150, 1, 'Tarde', 38.712, -9.130)")
        rawDb.execSQL("INSERT INTO collection_entry (id, containerCount, timestamp, estimatedValueCents, collectionSpotId, note, latitude, longitude) VALUES (2, 20, 1680005000000, 200, 1, 'Noite', 38.712, -9.130)")
        rawDb.execSQL("INSERT INTO redemption_entry (id, presentedContainers, acceptedContainers, rejectedContainers, actualRecoveredCents, timestamp, returnPointId, note) VALUES (1, 35, 35, 0, 350, 1680006000000, null, 'Redeemed')")
        rawDb.execSQL("INSERT INTO return_point (id, name, type, latitude, longitude, address, source, isVerified, lastUpdatedTimestamp, userNotes) VALUES (1, 'Supermercado Central', 'AUTOMATIC_MACHINE', 38.715, -9.135, 'Av Central', 'OFFICIAL_SDR', 1, 1680000000000, null)")
        rawDb.execSQL("INSERT INTO goal (id, type, targetValue, period, isActive) VALUES (1, 'CONTAINERS', 100, 'MONTHLY', 1)")
        rawDb.close()

        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_1_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_1_4,
                AppDatabase.MIGRATION_2_4
            )
            .build()

        val collections = roomDb.collectionDao().getAllCollections().first()
        assertEquals(2, collections.size)
        val entry1 = collections.first { it.id == 1 }
        assertEquals(15, entry1.containerCount)
        assertEquals(150L, entry1.estimatedValueCents)
        assertEquals(1, entry1.collectionSpotId)
        assertEquals(SyncState.LOCAL_ONLY.name, entry1.syncState)
        assertNotNull("MIGRATION_3_4 must assign a valid UUID remoteId", entry1.remoteId)
        assertTrue(entry1.remoteId!!.isNotEmpty())
        assertNull(entry1.deletedAt)

        val spots = roomDb.spotDao().getAllSpots().first()
        assertEquals(1, spots.size)
        assertEquals("Miradouro", spots[0].name)
        assertNotNull(spots[0].remoteId)

        val redemptions = roomDb.redemptionDao().getAllRedemptions().first()
        assertEquals(1, redemptions.size)
        assertEquals(35, redemptions[0].acceptedContainers)

        val profile = roomDb.userDao().getUserProfile().first()
        assertNotNull(profile)
        assertEquals("Maria", profile!!.name)
        assertNotNull(profile.remoteId)

        // Insert new entry with automatic UUID and verify outbox DAO works
        val newId = roomDb.collectionDao().insertCollection(
            CollectionEntryEntity(
                containerCount = 8,
                timestamp = System.currentTimeMillis(),
                estimatedValueCents = 80L,
                note = "Nova recolha pós-migração"
            )
        )
        assertTrue(newId > 0)
        assertEquals(3, roomDb.collectionDao().getAllCollections().first().size)

        roomDb.close()
    }
}
