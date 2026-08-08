package com.dmitriim.localaiplayground.source.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        InstalledModelEntity::class,
        ModelTransferEntity::class,
        ModelTransferFileEntity::class,
        RunEntity::class,
        ConversationEntity::class,
        ConversationMessageEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppModelDatabase : RoomDatabase() {
    abstract fun installedModelDao(): InstalledModelDao
    abstract fun modelTransferDao(): ModelTransferDao
    abstract fun runDao(): RunDao
    abstract fun conversationDao(): ConversationDao
}
