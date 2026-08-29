package com.dmitriim.localailab.feature.device.impl.navigation

import androidx.compose.runtime.Composable
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.core.navigation.AppDestination
import com.dmitriim.localailab.core.navigation.NavigationEntryProvider
import com.dmitriim.localailab.core.navigation.TopLevelDestination
import com.dmitriim.localailab.feature.device.api.navigation.DeviceDestination
import com.dmitriim.localailab.feature.device.impl.presentation.DeviceRoute
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
@Inject
@ContributesIntoSet(AppScope::class, binding<NavigationEntryProvider>())
class DeviceNavigationEntryProvider : NavigationEntryProvider {
    override val destinationType = DeviceDestination::class
    override val hostDestination = TopLevelDestination.SETTINGS

    @Composable
    override fun Content(destination: AppDestination, navigator: AppNavigator) {
        DeviceRoute()
    }
}
