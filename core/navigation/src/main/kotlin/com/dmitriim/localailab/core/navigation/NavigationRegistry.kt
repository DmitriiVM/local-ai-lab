package com.dmitriim.localailab.core.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey

internal class NavigationRegistry(
    providers: Set<NavigationEntryProvider>,
) {
    private val providersByDestinationType = providers.associateBy { it.destinationType }
    private val rootDestinationsByTopLevel: Map<TopLevelDestination, AppDestination> = buildMap {
        providers.forEach { provider ->
            val rootDestination = provider.rootDestination ?: return@forEach
            check(provider.destinationType.isInstance(rootDestination)) {
                "Root destination $rootDestination must match ${provider.destinationType}."
            }
            check(put(provider.hostDestination, rootDestination) == null) {
                "Top-level destination ${provider.hostDestination} must have exactly one root destination."
            }
        }
    }

    init {
        check(providersByDestinationType.size == providers.size) {
            "Every destination type must have exactly one navigation entry provider."
        }
        check(rootDestinationsByTopLevel.size == TopLevelDestination.entries.size) {
            "Every top-level destination must have exactly one root destination."
        }
        TopLevelDestination.entries.forEach { destination ->
            checkNotNull(rootDestinationsByTopLevel[destination]) {
                "Missing root destination for $destination"
            }
        }
    }

    fun rootDestination(destination: TopLevelDestination): AppDestination = checkNotNull(rootDestinationsByTopLevel[destination])

    fun hostDestinationFor(destination: AppDestination): TopLevelDestination = providerFor(destination).hostDestination

    fun entryFor(key: NavKey, navigator: AppNavigator): NavEntry<NavKey>? = (key as? AppDestination)?.let { destination ->
        val provider = providerFor(destination)
        NavEntry(destination) {
            provider.Content(destination, navigator)
        }
    }

    private fun providerFor(destination: AppDestination): NavigationEntryProvider = checkNotNull(
        providersByDestinationType[destination::class],
    ) {
        "Missing navigation entry provider for ${destination::class}."
    }
}
