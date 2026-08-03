package com.dmitriim.localaiplayground.source.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        InstalledModelEntity::class,
        RunEntity::class,
        ConversationEntity::class,
        ConversationMessageEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppModelDatabase : RoomDatabase() {
    abstract fun installedModelDao(): InstalledModelDao
    abstract fun runDao(): RunDao
    abstract fun conversationDao(): ConversationDao
}
