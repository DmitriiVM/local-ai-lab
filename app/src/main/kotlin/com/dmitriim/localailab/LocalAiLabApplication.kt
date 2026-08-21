package com.dmitriim.localailab

import android.app.Application
import android.content.ComponentCallbacks2
import com.dmitriim.localailab.core.result.ForegroundOperationInterruption
import com.dmitriim.localailab.di.AppGraph
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LocalAiLabApplication : Application() {
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

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            graph.foregroundOperationCoordinator.interruptActiveOperations(
                ForegroundOperationInterruption.MEMORY_PRESSURE,
            )
            graph.runtimeMemoryManager.evictAll()
        }
    }
}
