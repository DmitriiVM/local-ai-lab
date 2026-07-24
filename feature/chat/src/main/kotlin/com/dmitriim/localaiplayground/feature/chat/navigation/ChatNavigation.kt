package com.dmitriim.localaiplayground.feature.chat.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.navigation.AppNavigator
import com.dmitriim.localaiplayground.core.navigation.NavigationEntryProvider
import com.dmitriim.localaiplayground.core.navigation.NavigationTarget
import com.dmitriim.localaiplayground.core.navigation.TopLevelDestination
import com.dmitriim.localaiplayground.feature.chat.presentation.ChatRoute
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.serialization.Serializable

@Serializable
data object ChatKey : NavKey

@Inject
@ContributesIntoSet(AppScope::class, binding<NavigationEntryProvider>())
class ChatNavigationEntryProvider : NavigationEntryProvider {
    override val target = NavigationTarget.CHAT
    override val hostDestination = TopLevelDestination.PLAYGROUND
    override val startKey: NavKey = ChatKey

    override fun entryFor(key: NavKey, navigator: AppNavigator): NavEntry<NavKey>? =
        if (key == ChatKey) NavEntry(key) { ChatRoute() } else null
}
