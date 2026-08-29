package com.dmitriim.localailab.feature.runs.impl.data.persistence

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RunEntity::class,
        ConversationEntity::class,
        ConversationMessageEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class RunsDatabase : RoomDatabase() {
    abstract fun runDao(): RunDao

    abstract fun conversationDao(): ConversationDao
}
