package com.dmitriim.localailab.feature.stt.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.core.navigation.NavigationEntryProvider
import com.dmitriim.localailab.core.navigation.NavigationTarget
import com.dmitriim.localailab.core.navigation.TopLevelDestination
import com.dmitriim.localailab.feature.stt.presentation.SpeechToTextRoute
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.serialization.Serializable

@Serializable
data object SpeechToTextKey : NavKey

@Inject
@ContributesIntoSet(AppScope::class, binding<NavigationEntryProvider>())
class SpeechToTextNavigationEntryProvider : NavigationEntryProvider {
    override val target = NavigationTarget.SPEECH_TO_TEXT
    override val hostDestination = TopLevelDestination.PLAYGROUND
    override val startKey: NavKey = SpeechToTextKey

    override fun entryFor(key: NavKey, navigator: AppNavigator): NavEntry<NavKey>? = if (key == SpeechToTextKey) {
        NavEntry(key) { SpeechToTextRoute(navigator) }
    } else {
        null
    }
}
