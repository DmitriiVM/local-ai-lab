package com.dmitriim.localailab.feature.playground.navigation

import androidx.compose.runtime.Composable
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.core.navigation.AppDestination
import com.dmitriim.localailab.core.navigation.NavigationEntryProvider
import com.dmitriim.localailab.core.navigation.TopLevelDestination
import com.dmitriim.localailab.core.navigation.destination.PlaygroundDestination
import com.dmitriim.localailab.feature.playground.presentation.PlaygroundRoute
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
@Inject
@ContributesIntoSet(AppScope::class, binding<NavigationEntryProvider>())
class PlaygroundNavigationEntryProvider : NavigationEntryProvider {
    override val destinationType = PlaygroundDestination::class
    override val hostDestination = TopLevelDestination.PLAYGROUND
    override val isRootDestination = true

    @Composable
    override fun Content(destination: AppDestination, navigator: AppNavigator) {
        PlaygroundRoute(navigator)
    }
}
