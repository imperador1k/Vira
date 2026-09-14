package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        LocalUserProfileEntity::class,
        CollectionEntryEntity::class,
        CollectionSpotEntity::class,
        RedemptionEntryEntity::class,
        ReturnPointEntity::class,
        GoalEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
    abstract fun redemptionDao(): RedemptionDao
    abstract fun userDao(): UserDao
    abstract fun spotDao(): CollectionSpotDao
}
