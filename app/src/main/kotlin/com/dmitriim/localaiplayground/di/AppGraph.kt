package com.dmitriim.localaiplayground.di

import android.app.Application
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.di.ApplicationCoroutineScope
import com.dmitriim.localaiplayground.core.navigation.NavigationEntryProvider
import com.dmitriim.localaiplayground.core.model.ModelTransfers
import com.dmitriim.localaiplayground.core.result.ForegroundOperationCoordinator
import com.dmitriim.localaiplayground.source.settings.AppSettingsRepository
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
