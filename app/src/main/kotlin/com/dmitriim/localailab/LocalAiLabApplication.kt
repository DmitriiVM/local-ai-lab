package com.dmitriim.localailab

import android.app.Application
import android.os.Build
import com.dmitriim.localailab.core.operation.ForegroundOperationInterruption
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
        graph.modelTransferStartup.initialize()
    }

    @Suppress("DEPRECATION") // Running-low callbacks are not delivered on Android 14+.
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            level >= TRIM_MEMORY_RUNNING_LOW
        ) {
            graph.foregroundOperationCoordinator.interruptActiveOperations(
                ForegroundOperationInterruption.MEMORY_PRESSURE,
            )
            graph.runtimeLeaseManager.evictAll()
        }
    }
}
