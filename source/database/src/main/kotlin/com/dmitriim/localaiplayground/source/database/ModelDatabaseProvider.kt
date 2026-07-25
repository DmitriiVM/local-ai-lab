package com.dmitriim.localaiplayground.source.database

import android.app.Application
import androidx.room.Room
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
    ).build()
}
