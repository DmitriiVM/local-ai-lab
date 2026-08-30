package com.dmitriim.localailab.feature.models.impl.models.data.persistence

import android.app.Application
import androidx.room.Room
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
class ModelsDatabaseProvider(application: Application) {
    val database: ModelsDatabase = Room.databaseBuilder(
        application,
        ModelsDatabase::class.java,
        DATABASE_NAME,
    ).build()

    private companion object {
        const val DATABASE_NAME = "local-ai-models.db"
    }
}
