package com.dmitriim.localailab.feature.assistant.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.core.navigation.NavigationEntryProvider
import com.dmitriim.localailab.core.navigation.NavigationTarget
import com.dmitriim.localailab.core.navigation.TopLevelDestination
import com.dmitriim.localailab.feature.assistant.presentation.AssistantRoute
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.serialization.Serializable

@Serializable
data object AssistantKey : NavKey

@Inject
@ContributesIntoSet(AppScope::class, binding<NavigationEntryProvider>())
class AssistantNavigationEntryProvider : NavigationEntryProvider {
    override val target = NavigationTarget.ASSISTANT
    override val hostDestination = TopLevelDestination.PLAYGROUND
    override val startKey: NavKey = AssistantKey

    override fun entryFor(key: NavKey, navigator: AppNavigator): NavEntry<NavKey>? = if (key == AssistantKey) NavEntry(key) { AssistantRoute(navigator) } else null
}
