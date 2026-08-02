package com.dmitriim.localaiplayground.feature.settings.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.navigation.AppNavigator
import com.dmitriim.localaiplayground.core.navigation.NavigationEntryProvider
import com.dmitriim.localaiplayground.core.navigation.NavigationTarget
import com.dmitriim.localaiplayground.core.navigation.TopLevelDestination
import com.dmitriim.localaiplayground.feature.settings.presentation.SettingsRoute
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.serialization.Serializable

@Serializable
data object SettingsKey : NavKey

@Inject
@ContributesIntoSet(AppScope::class, binding<NavigationEntryProvider>())
class SettingsNavigationEntryProvider : NavigationEntryProvider {
    override val target = NavigationTarget.SETTINGS
    override val topLevelDestination = TopLevelDestination.SETTINGS
    override val startKey: NavKey = SettingsKey

    override fun entryFor(key: NavKey, navigator: AppNavigator): NavEntry<NavKey>? = if (key == SettingsKey) {
        NavEntry(key) {
            SettingsRoute(
                onOpenDeviceAndRuntimes = { navigator.navigate(NavigationTarget.DEVICE) },
            )
        }
    } else {
        null
    }
}
