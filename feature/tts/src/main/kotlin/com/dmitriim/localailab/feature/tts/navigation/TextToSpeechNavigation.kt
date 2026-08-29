package com.dmitriim.localailab.feature.tts.navigation

import androidx.compose.runtime.Composable
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.navigation.AppDestination
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.core.navigation.NavigationEntryProvider
import com.dmitriim.localailab.core.navigation.TopLevelDestination
import com.dmitriim.localailab.core.navigation.destination.TextToSpeechDestination
import com.dmitriim.localailab.feature.tts.presentation.TextToSpeechRoute
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding<NavigationEntryProvider>())
class TextToSpeechNavigationEntryProvider : NavigationEntryProvider {
    override val destinationType = TextToSpeechDestination::class
    override val hostDestination = TopLevelDestination.PLAYGROUND

    @Composable
    override fun Content(destination: AppDestination, navigator: AppNavigator) {
        TextToSpeechRoute(navigator)
    }
}
