package com.dmitriim.localailab.feature.assistant.impl.navigation

import androidx.compose.runtime.Composable
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.navigation.AppDestination
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.core.navigation.NavigationEntryProvider
import com.dmitriim.localailab.core.navigation.TopLevelDestination
import com.dmitriim.localailab.feature.assistant.api.navigation.AssistantDestination
import com.dmitriim.localailab.feature.assistant.impl.presentation.AssistantRoute
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
@Inject
@ContributesIntoSet(AppScope::class, binding<NavigationEntryProvider>())
class AssistantNavigationEntryProvider : NavigationEntryProvider {
    override val destinationType = AssistantDestination::class
    override val hostDestination = TopLevelDestination.PLAYGROUND

    @Composable
    override fun Content(destination: AppDestination, navigator: AppNavigator) {
        AssistantRoute(navigator)
    }
}
