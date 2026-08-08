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
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()

    private companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE runs ADD COLUMN kind TEXT NOT NULL DEFAULT 'INFERENCE'")
                database.execSQL("ALTER TABLE runs ADD COLUMN benchmarkSessionId TEXT")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_runs_benchmarkSessionId ON runs (benchmarkSessionId)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS model_transfers (" +
                        "modelId TEXT NOT NULL, catalogVersion INTEGER NOT NULL, revision TEXT, " +
                        "status TEXT NOT NULL, networkPolicy TEXT NOT NULL, executionGeneration INTEGER NOT NULL, " +
                        "completedBytes INTEGER NOT NULL, totalBytes INTEGER NOT NULL, currentRelativePath TEXT, " +
                        "message TEXT, updatedAtEpochMs INTEGER NOT NULL, PRIMARY KEY(modelId))",
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS model_transfer_files (" +
                        "modelId TEXT NOT NULL, relativePath TEXT NOT NULL, eTag TEXT, lastModified TEXT, " +
                        "verified INTEGER NOT NULL, PRIMARY KEY(modelId, relativePath))",
                )
            }
        }
    }
}
