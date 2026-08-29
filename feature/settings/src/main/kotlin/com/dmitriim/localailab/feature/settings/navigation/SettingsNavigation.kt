package com.dmitriim.localailab.feature.settings.navigation

import androidx.compose.runtime.Composable
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.navigation.AppDestination
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.core.navigation.NavigationEntryProvider
import com.dmitriim.localailab.core.navigation.TopLevelDestination
import com.dmitriim.localailab.core.navigation.destination.DeviceDestination
import com.dmitriim.localailab.core.navigation.destination.SettingsDestination
import com.dmitriim.localailab.feature.settings.presentation.SettingsRoute
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding<NavigationEntryProvider>())
class SettingsNavigationEntryProvider : NavigationEntryProvider {
    override val destinationType = SettingsDestination::class
    override val hostDestination = TopLevelDestination.SETTINGS
    override val isRootDestination = true

    @Composable
    override fun Content(destination: AppDestination, navigator: AppNavigator) {
        SettingsRoute(
            onOpenDeviceAndRuntimes = { navigator.navigate(DeviceDestination) },
        )
    }
}
