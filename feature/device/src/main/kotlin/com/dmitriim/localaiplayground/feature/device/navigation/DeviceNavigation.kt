package com.dmitriim.localaiplayground.feature.device.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.navigation.AppNavigator
import com.dmitriim.localaiplayground.core.navigation.NavigationEntryProvider
import com.dmitriim.localaiplayground.core.navigation.NavigationTarget
import com.dmitriim.localaiplayground.core.navigation.TopLevelDestination
import com.dmitriim.localaiplayground.feature.device.presentation.DeviceRoute
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.serialization.Serializable

@Serializable
data object DeviceKey : NavKey

@Inject
@ContributesIntoSet(AppScope::class, binding<NavigationEntryProvider>())
class DeviceNavigationEntryProvider : NavigationEntryProvider {
    override val target = NavigationTarget.DEVICE
    override val hostDestination = TopLevelDestination.SETTINGS
    override val startKey: NavKey = DeviceKey

    override fun entryFor(key: NavKey, navigator: AppNavigator): NavEntry<NavKey>? = if (key == DeviceKey) NavEntry(key) { DeviceRoute() } else null
}
