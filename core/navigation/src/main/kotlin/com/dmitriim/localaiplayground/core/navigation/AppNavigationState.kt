package com.dmitriim.localaiplayground.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

@Composable
fun rememberAppNavigationState(
    providers: Set<NavigationEntryProvider>,
    startDestination: TopLevelDestination = TopLevelDestination.PLAYGROUND,
): AppNavigationState {
    val registry = remember(providers) { NavigationRegistry(providers) }
    val playgroundStack = rememberNavBackStack(
        registry.startKey(TopLevelDestination.PLAYGROUND),
    )
    val modelsStack = rememberNavBackStack(
        registry.startKey(TopLevelDestination.MODELS),
    )
    val runsStack = rememberNavBackStack(
        registry.startKey(TopLevelDestination.RUNS),
    )
    val deviceStack = rememberNavBackStack(
        registry.startKey(TopLevelDestination.DEVICE),
    )
    val stacks = remember(playgroundStack, modelsStack, runsStack, deviceStack) {
        TopLevelBackStacks(
            playground = playgroundStack,
            models = modelsStack,
            runs = runsStack,
            device = deviceStack,
        )
    }
    val selectedDestination = rememberSaveable {
        androidx.compose.runtime.mutableStateOf(startDestination)
    }

    return remember(registry, stacks, selectedDestination, startDestination) {
        AppNavigationState(
            registry = registry,
            stacks = stacks,
            selectedDestinationState = selectedDestination,
            startDestination = startDestination,
        )
    }
}

class AppNavigationState internal constructor(
    private val registry: NavigationRegistry,
    private val stacks: TopLevelBackStacks,
    private val selectedDestinationState: MutableState<TopLevelDestination>,
    private val startDestination: TopLevelDestination,
) : AppNavigator {
    var selectedDestination by selectedDestinationState
        private set

    val activeStack: NavBackStack<NavKey>
        get() = stacks[selectedDestination]

    val shouldHandleSystemBack: Boolean
        get() = activeStack.size == 1 && selectedDestination != startDestination

    val canNavigateUp: Boolean
        get() = activeStack.size > 1

    fun selectTopLevelDestination(destination: TopLevelDestination) {
        selectedDestination = destination
    }

    override fun navigate(target: NavigationTarget) {
        val provider = registry.provider(target)
        provider.topLevelDestination?.let { destination ->
            selectedDestination = destination
            return
        }

        val host = provider.hostDestination ?: selectedDestination
        selectedDestination = host
        val stack = stacks[host]
        if (stack.lastOrNull() != provider.startKey) {
            stack.add(provider.startKey)
        }
    }

    override fun navigateBack() {
        if (activeStack.size > 1) {
            activeStack.removeLastOrNull()
        } else if (selectedDestination != startDestination) {
            selectedDestination = startDestination
        }
    }

    internal fun entryFor(key: NavKey): NavEntry<NavKey>? =
        registry.entryFor(key, this)
}

internal data class TopLevelBackStacks(
    val playground: NavBackStack<NavKey>,
    val models: NavBackStack<NavKey>,
    val runs: NavBackStack<NavKey>,
    val device: NavBackStack<NavKey>,
) {
    operator fun get(destination: TopLevelDestination): NavBackStack<NavKey> =
        when (destination) {
            TopLevelDestination.PLAYGROUND -> playground
            TopLevelDestination.MODELS -> models
            TopLevelDestination.RUNS -> runs
            TopLevelDestination.DEVICE -> device
        }
}
