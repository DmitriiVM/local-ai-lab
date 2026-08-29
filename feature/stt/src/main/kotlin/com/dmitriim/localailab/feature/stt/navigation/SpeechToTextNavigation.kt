package com.dmitriim.localailab.feature.stt.navigation

import androidx.compose.runtime.Composable
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.navigation.AppDestination
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.core.navigation.NavigationEntryProvider
import com.dmitriim.localailab.core.navigation.TopLevelDestination
import com.dmitriim.localailab.core.navigation.destination.SpeechToTextDestination
import com.dmitriim.localailab.feature.stt.presentation.SpeechToTextRoute
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
@Inject
@ContributesIntoSet(AppScope::class, binding<NavigationEntryProvider>())
class SpeechToTextNavigationEntryProvider : NavigationEntryProvider {
    override val destinationType = SpeechToTextDestination::class
    override val hostDestination = TopLevelDestination.PLAYGROUND

    @Composable
    override fun Content(destination: AppDestination, navigator: AppNavigator) {
        SpeechToTextRoute(navigator)
    }
}
