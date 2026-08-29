package com.dmitriim.localailab.feature.runs.impl.data.persistence

import android.app.Application
import androidx.room.Room
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
class RunsDatabaseProvider(application: Application) {
    val database: RunsDatabase = Room.databaseBuilder(
        application,
        RunsDatabase::class.java,
        DATABASE_NAME,
    ).build()

    private companion object {
        const val DATABASE_NAME = "local-ai-runs.db"
    }
}
