package com.dmitriim.localailab.core.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey

internal class NavigationRegistry(
    providers: Set<NavigationEntryProvider>,
) {
    private val providersByTarget = providers.associateBy { it.target }
    private val allProviders = providers.toList()

    init {
        check(providersByTarget.size == providers.size) {
            "Every navigation target must have exactly one module-owned contribution."
        }
        NavigationTarget.entries.forEach { target ->
            checkNotNull(providersByTarget[target]) {
                "Missing navigation contribution for $target"
            }
        }
    }

    fun provider(target: NavigationTarget): NavigationEntryProvider = checkNotNull(providersByTarget[target])

    fun startKey(destination: TopLevelDestination): NavKey = allProviders.single { it.topLevelDestination == destination }.startKey

    fun entryFor(key: NavKey, navigator: AppNavigator): NavEntry<NavKey>? = allProviders.firstNotNullOfOrNull { provider ->
        provider.entryFor(key, navigator)
    }
}
