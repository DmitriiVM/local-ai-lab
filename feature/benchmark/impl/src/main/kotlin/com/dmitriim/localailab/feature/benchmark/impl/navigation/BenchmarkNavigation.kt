package com.dmitriim.localailab.feature.benchmark.impl.navigation

import androidx.compose.runtime.Composable
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.navigation.AppDestination
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.core.navigation.NavigationEntryProvider
import com.dmitriim.localailab.core.navigation.TopLevelDestination
import com.dmitriim.localailab.feature.benchmark.api.navigation.BenchmarkDestination
import com.dmitriim.localailab.feature.benchmark.impl.presentation.BenchmarkRoute
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
@Inject
@ContributesIntoSet(AppScope::class, binding<NavigationEntryProvider>())
class BenchmarkNavigationEntryProvider : NavigationEntryProvider {
    override val destinationType = BenchmarkDestination::class
    override val hostDestination = TopLevelDestination.PLAYGROUND

    @Composable
    override fun Content(destination: AppDestination, navigator: AppNavigator) {
        BenchmarkRoute()
    }
}
