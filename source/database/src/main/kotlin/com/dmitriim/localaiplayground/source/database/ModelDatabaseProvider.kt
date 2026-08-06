package com.dmitriim.localaiplayground.source.database

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dmitriim.localaiplayground.core.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
class ModelDatabaseProvider(application: Application) {
    val database: AppModelDatabase = Room.databaseBuilder(
        application,
        AppModelDatabase::class.java,
        "local-ai-playground.db",
    ).addMigrations(MIGRATION_1_2).build()

    private companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE runs ADD COLUMN kind TEXT NOT NULL DEFAULT 'INFERENCE'")
                database.execSQL("ALTER TABLE runs ADD COLUMN benchmarkSessionId TEXT")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_runs_benchmarkSessionId ON runs (benchmarkSessionId)")
            }
        }
    }
}
