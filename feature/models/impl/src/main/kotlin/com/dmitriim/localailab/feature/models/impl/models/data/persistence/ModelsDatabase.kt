package com.dmitriim.localailab.feature.models.impl.models.data.persistence

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        InstalledModelEntity::class,
        ModelTransferEntity::class,
        ModelTransferFileEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ModelsDatabase : RoomDatabase() {
    abstract fun installedModelDao(): InstalledModelDao

    abstract fun modelTransferDao(): ModelTransferDao

    abstract fun modelTransferFileDao(): ModelTransferFileDao
}
