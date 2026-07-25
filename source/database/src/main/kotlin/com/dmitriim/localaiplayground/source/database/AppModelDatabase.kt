package com.dmitriim.localaiplayground.source.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [InstalledModelEntity::class], version = 1, exportSchema = false)
abstract class AppModelDatabase : RoomDatabase() {
    abstract fun installedModelDao(): InstalledModelDao
}
