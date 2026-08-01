package com.dmitriim.localaiplayground.feature.tts.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.navigation.AppNavigator
import com.dmitriim.localaiplayground.core.navigation.NavigationEntryProvider
import com.dmitriim.localaiplayground.core.navigation.NavigationTarget
import com.dmitriim.localaiplayground.core.navigation.TopLevelDestination
import com.dmitriim.localaiplayground.feature.tts.presentation.TextToSpeechRoute
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding<NavigationEntryProvider>())
class TextToSpeechNavigationEntryProvider : NavigationEntryProvider {
    override val target = NavigationTarget.TEXT_TO_SPEECH
    override val hostDestination = TopLevelDestination.PLAYGROUND
    override val startKey: NavKey = TextToSpeechKey

    override fun entryFor(key: NavKey, navigator: AppNavigator): NavEntry<NavKey>? =
        if (key == TextToSpeechKey) NavEntry(key) { TextToSpeechRoute() } else null
}
