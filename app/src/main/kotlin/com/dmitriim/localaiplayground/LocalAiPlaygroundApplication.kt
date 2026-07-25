package com.dmitriim.localaiplayground

import android.app.Application
import com.dmitriim.localaiplayground.di.AppGraph
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LocalAiPlaygroundApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val graph: AppGraph by lazy {
        createGraphFactory<AppGraph.Factory>().create(
            application = this,
            applicationScope = applicationScope,
        )
    }

    override fun onCreate() {
        super.onCreate()
        graph.modelTransfers
    }

}
