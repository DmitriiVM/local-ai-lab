package com.dmitriim.localailab.feature.tts.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.core.navigation.NavigationEntryProvider
import com.dmitriim.localailab.core.navigation.NavigationTarget
import com.dmitriim.localailab.core.navigation.TopLevelDestination
import com.dmitriim.localailab.feature.tts.presentation.TextToSpeechRoute
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding<NavigationEntryProvider>())
class TextToSpeechNavigationEntryProvider : NavigationEntryProvider {
    override val target = NavigationTarget.TEXT_TO_SPEECH
    override val hostDestination = TopLevelDestination.PLAYGROUND
    override val startKey: NavKey = TextToSpeechKey

    override fun entryFor(key: NavKey, navigator: AppNavigator): NavEntry<NavKey>? = if (key == TextToSpeechKey) {
        NavEntry(key) { TextToSpeechRoute(navigator) }
    } else {
        null
    }
}
