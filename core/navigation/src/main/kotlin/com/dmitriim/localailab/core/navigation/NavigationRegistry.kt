package com.dmitriim.localailab.core.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey

internal class NavigationRegistry(
    providers: Set<NavigationEntryProvider>,
) {
    private val providersByDestinationType = providers.associateBy { it.destinationType }
    private val rootProvidersByHost: Map<TopLevelDestination, NavigationEntryProvider> = buildMap {
        providers.forEach { provider ->
            if (!provider.isRootDestination) return@forEach
            check(put(provider.hostDestination, provider) == null) {
                "Top-level host ${provider.hostDestination} must have exactly one root provider."
            }
        }
    }

    init {
        check(providersByDestinationType.size == providers.size) {
            "Every destination type must have exactly one navigation entry provider."
        }
        check(rootProvidersByHost.size == TopLevelDestination.entries.size) {
            "Every top-level host must have exactly one root provider."
        }
        TopLevelDestination.entries.forEach { hostDestination ->
            checkNotNull(rootProvidersByHost[hostDestination]) {
                "Missing root provider for $hostDestination"
            }
        }
    }

    fun hostDestinationFor(destination: AppDestination): TopLevelDestination = providerFor(destination).hostDestination

    fun isRootDestination(destination: AppDestination): Boolean = providerFor(destination).isRootDestination

    fun entryFor(
        key: NavKey,
        navigator: AppNavigator,
    ): NavEntry<NavKey>? = (key as? AppDestination)?.let { destination ->
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
