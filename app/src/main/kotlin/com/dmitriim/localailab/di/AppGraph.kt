package com.dmitriim.localailab.di

import android.app.Application
import com.dmitriim.localailab.ai.api.memory.AiRuntimeLeaseManager
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.di.ApplicationCoroutineScope
import com.dmitriim.localailab.core.model.service.ModelTransfers
import com.dmitriim.localailab.core.navigation.NavigationEntryProvider
import com.dmitriim.localailab.core.result.ForegroundOperationCoordinator
import com.dmitriim.localailab.source.settings.AppSettingsRepository
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import kotlinx.coroutines.CoroutineScope

@DependencyGraph(AppScope::class)
interface AppGraph : ViewModelGraph {
    val navigationEntryProviders: Set<NavigationEntryProvider>
    val foregroundOperationCoordinator: ForegroundOperationCoordinator
    val modelTransfers: ModelTransfers
    val settingsRepository: AppSettingsRepository
    val runtimeLeaseManager: AiRuntimeLeaseManager

    @get:ApplicationCoroutineScope
    val applicationScope: CoroutineScope

    @DependencyGraph.Factory
    interface Factory {
        fun create(
            @Provides application: Application,
            @Provides @ApplicationCoroutineScope applicationScope: CoroutineScope,
        ): AppGraph
    }
}
