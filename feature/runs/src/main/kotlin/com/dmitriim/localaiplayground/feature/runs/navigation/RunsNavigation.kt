package com.dmitriim.localaiplayground.feature.runs.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.navigation.AppNavigator
import com.dmitriim.localaiplayground.core.navigation.NavigationEntryProvider
import com.dmitriim.localaiplayground.core.navigation.NavigationTarget
import com.dmitriim.localaiplayground.core.navigation.TopLevelDestination
import com.dmitriim.localaiplayground.feature.runs.presentation.RunsRoute
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.serialization.Serializable

@Serializable
data object RunsKey : NavKey

@Inject
@ContributesIntoSet(AppScope::class, binding<NavigationEntryProvider>())
class RunsNavigationEntryProvider : NavigationEntryProvider {
    override val target = NavigationTarget.RUNS
    override val topLevelDestination = TopLevelDestination.RUNS
    override val startKey: NavKey = RunsKey

    override fun entryFor(key: NavKey, navigator: AppNavigator): NavEntry<NavKey>? = if (key == RunsKey) NavEntry(key) { RunsRoute(navigator) } else null
}
