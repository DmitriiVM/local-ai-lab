package com.dmitriim.localailab.feature.models.impl.models.navigation

import androidx.compose.runtime.Composable
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.navigation.AppDestination
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.core.navigation.NavigationEntryProvider
import com.dmitriim.localailab.core.navigation.TopLevelDestination
import com.dmitriim.localailab.feature.models.api.navigation.ModelsDestination
import com.dmitriim.localailab.feature.models.impl.models.presentation.ModelsRoute
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding<NavigationEntryProvider>())
class ModelsNavigationEntryProvider : NavigationEntryProvider {
    override val destinationType = ModelsDestination::class
    override val hostDestination = TopLevelDestination.MODELS
    override val isRootDestination = true

    @Composable
    override fun Content(destination: AppDestination, navigator: AppNavigator) {
        ModelsRoute(navigator)
    }
}
