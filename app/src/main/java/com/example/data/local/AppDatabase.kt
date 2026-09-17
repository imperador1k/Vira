package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Database(
    entities = [
        LocalUserProfileEntity::class,
        CollectionEntryEntity::class,
        CollectionSpotEntity::class,
        RedemptionEntryEntity::class,
        ReturnPointEntity::class,
        FavoriteReturnPointEntity::class,
        GoalEntity::class,
        SyncOutboxEntity::class,
        SyncMetadataEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
    abstract fun redemptionDao(): RedemptionDao
    abstract fun userDao(): UserDao
    abstract fun spotDao(): CollectionSpotDao
    abstract fun returnPointDao(): ReturnPointDao
    abstract fun favoriteReturnPointDao(): FavoriteReturnPointDao
    abstract fun goalDao(): GoalDao
    abstract fun syncOutboxDao(): SyncOutboxDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    suspend fun clearPersonalDatasetAndRebind(newOwnerUserId: String) {
        this.withTransaction {
            val personalTables = listOf(
                "collection_entry",
                "collection_spot",
                "redemption_entry",
                "goal",
                "user_profile",
                "favorite_return_point",
                "sync_outbox"
            )
            for (table in personalTables) {
                openHelper.writableDatabase.execSQL("DELETE FROM $table")
            }

            syncMetadataDao().deleteKey("sync_cursor_version")

            val now = System.currentTimeMillis()
            syncMetadataDao().setValue(
                SyncMetadataEntity(
                    key = "dataset_owner_user_id",
                    value = newOwnerUserId,
                    updatedAt = now
                )
            )
            syncMetadataDao().setValue(
                SyncMetadataEntity(
                    key = "dataset_linked_at",
                    value = now.toString(),
                    updatedAt = now
                )
            )
            syncMetadataDao().setValue(
                SyncMetadataEntity(
                    key = "dataset_binding_version",
                    value = "1",
                    updatedAt = now
                )
            )
        }
    }

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS return_point_v2 (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        type TEXT NOT NULL,
                        source TEXT NOT NULL,
                        verificationStatus TEXT NOT NULL,
                        lastVerifiedAt INTEGER,
                        address TEXT,
                        openingHours TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO return_point_v2 (
                        id, name, latitude, longitude, type, source,
                        verificationStatus, lastVerifiedAt, address, openingHours, createdAt, updatedAt
                    )
                    SELECT 
                        id, name, latitude, longitude, type, source,
                        CASE WHEN isVerified = 1 THEN 'VERIFIED' ELSE 'UNVERIFIED' END,
                        CASE WHEN isVerified = 1 THEN lastUpdatedTimestamp ELSE NULL END,
                        address,
                        NULL,
                        lastUpdatedTimestamp,
                        lastUpdatedTimestamp
                    FROM return_point
                """.trimIndent())

                db.execSQL("DROP TABLE return_point")
                db.execSQL("ALTER TABLE return_point_v2 RENAME TO return_point")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. user_profile
                db.execSQL("ALTER TABLE user_profile ADD COLUMN remoteId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
                db.execSQL("UPDATE user_profile SET updatedAt = memberSince WHERE updatedAt = 0")

                // 2. collection_entry
                db.execSQL("ALTER TABLE collection_entry ADD COLUMN remoteId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE collection_entry ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE collection_entry ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE collection_entry ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE collection_entry ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
                db.execSQL("UPDATE collection_entry SET createdAt = timestamp, updatedAt = timestamp WHERE createdAt = 0")

                // 3. redemption_entry
                db.execSQL("ALTER TABLE redemption_entry ADD COLUMN remoteId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE redemption_entry ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE redemption_entry ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE redemption_entry ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE redemption_entry ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
                db.execSQL("UPDATE redemption_entry SET createdAt = timestamp, updatedAt = timestamp WHERE createdAt = 0")

                // 4. collection_spot
                db.execSQL("ALTER TABLE collection_spot ADD COLUMN remoteId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE collection_spot ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE collection_spot ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE collection_spot ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
                db.execSQL("UPDATE collection_spot SET updatedAt = createdAt WHERE updatedAt = 0")

                // 5. return_point
                db.execSQL("ALTER TABLE return_point ADD COLUMN remoteId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE return_point ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE return_point ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")

                db.execSQL("UPDATE return_point SET type = 'MANUAL_POINT' WHERE type = 'MANUAL_STORE' OR type = 'USER_ADDED'")
                db.execSQL("UPDATE return_point SET type = 'AUTOMATIC_MACHINE' WHERE type = 'DEDICATED_CENTER'")
                db.execSQL("UPDATE return_point SET source = 'OFFICIAL' WHERE source = 'OFFICIAL_SDR' OR source = 'MUNICIPAL'")
                db.execSQL("UPDATE return_point SET verificationStatus = 'COMMUNITY_CONFIRMED' WHERE verificationStatus = 'COMMUNITY_SUBMITTED'")
                db.execSQL("UPDATE return_point SET verificationStatus = 'DISPUTED' WHERE verificationStatus = 'FLAGGED'")

                // 6. favorite_return_point
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS favorite_return_point (
                        returnPointId INTEGER NOT NULL PRIMARY KEY,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        remoteId TEXT DEFAULT NULL,
                        deletedAt INTEGER DEFAULT NULL,
                        syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY'
                    )
                """.trimIndent())

                // 7. goal
                db.execSQL("ALTER TABLE goal ADD COLUMN remoteId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE goal ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE goal ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE goal ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE goal ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create sync_outbox table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_outbox (
                        operationId TEXT PRIMARY KEY NOT NULL,
                        entityType TEXT NOT NULL,
                        entityRemoteId TEXT NOT NULL,
                        operationType TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        retryCount INTEGER NOT NULL,
                        lastAttemptAt INTEGER,
                        lastError TEXT,
                        payloadVersion INTEGER NOT NULL,
                        payloadJson TEXT
                    )
                """.trimIndent())

                // 2. Add serverUpdatedAt and remoteVersion to tables, and normalize existing null remoteIds
                val tablesWithIntPk = listOf("collection_entry", "collection_spot", "redemption_entry", "goal")
                for (table in tablesWithIntPk) {
                    db.execSQL("ALTER TABLE $table ADD COLUMN serverUpdatedAt INTEGER DEFAULT NULL")
                    db.execSQL("ALTER TABLE $table ADD COLUMN remoteVersion INTEGER NOT NULL DEFAULT 0")

                    val cursor = db.query("SELECT id FROM $table WHERE remoteId IS NULL")
                    while (cursor.moveToNext()) {
                        val id = cursor.getInt(0)
                        val uuid = UUID.randomUUID().toString()
                        db.execSQL("UPDATE $table SET remoteId = ? WHERE id = ?", arrayOf<Any?>(uuid, id))
                    }
                    cursor.close()
                }

                // user_profile
                db.execSQL("ALTER TABLE user_profile ADD COLUMN serverUpdatedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN remoteVersion INTEGER NOT NULL DEFAULT 0")
                val userCursor = db.query("SELECT id FROM user_profile WHERE remoteId IS NULL")
                while (userCursor.moveToNext()) {
                    val id = userCursor.getInt(0)
                    val uuid = UUID.randomUUID().toString()
                    db.execSQL("UPDATE user_profile SET remoteId = ? WHERE id = ?", arrayOf<Any?>(uuid, id))
                }
                userCursor.close()

                // favorite_return_point
                db.execSQL("ALTER TABLE favorite_return_point ADD COLUMN serverUpdatedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE favorite_return_point ADD COLUMN remoteVersion INTEGER NOT NULL DEFAULT 0")
                val favCursor = db.query("SELECT returnPointId FROM favorite_return_point WHERE remoteId IS NULL")
                while (favCursor.moveToNext()) {
                    val returnPointId = favCursor.getInt(0)
                    val uuid = UUID.randomUUID().toString()
                    db.execSQL("UPDATE favorite_return_point SET remoteId = ? WHERE returnPointId = ?", arrayOf<Any?>(uuid, returnPointId))
                }
                favCursor.close()
            }
        }

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
            }
        }

        val MIGRATION_1_4 = object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
                MIGRATION_3_4.migrate(db)
            }
        }

        val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_2_3.migrate(db)
                MIGRATION_3_4.migrate(db)
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_metadata (
                        `key` TEXT NOT NULL PRIMARY KEY,
                        `value` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_1_5 = object : Migration(1, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_4.migrate(db)
                MIGRATION_4_5.migrate(db)
            }
        }

        val MIGRATION_2_5 = object : Migration(2, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_2_4.migrate(db)
                MIGRATION_4_5.migrate(db)
            }
        }

        val MIGRATION_3_5 = object : Migration(3, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_3_4.migrate(db)
                MIGRATION_4_5.migrate(db)
            }
        }
    }
}
