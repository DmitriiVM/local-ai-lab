package com.dmitriim.localailab.feature.playground.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.core.navigation.NavigationEntryProvider
import com.dmitriim.localailab.core.navigation.NavigationTarget
import com.dmitriim.localailab.core.navigation.TopLevelDestination
import com.dmitriim.localailab.feature.playground.presentation.PlaygroundRoute
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.serialization.Serializable

@Serializable
data object PlaygroundKey : NavKey

@Inject
@ContributesIntoSet(AppScope::class, binding<NavigationEntryProvider>())
class PlaygroundNavigationEntryProvider : NavigationEntryProvider {
    override val target = NavigationTarget.PLAYGROUND
    override val topLevelDestination = TopLevelDestination.PLAYGROUND
    override val startKey: NavKey = PlaygroundKey

    override fun entryFor(key: NavKey, navigator: AppNavigator): NavEntry<NavKey>? = if (key == PlaygroundKey) NavEntry(key) { PlaygroundRoute(navigator) } else null
}
