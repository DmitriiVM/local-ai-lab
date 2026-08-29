package com.dmitriim.localailab.core.navigation

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
import com.dmitriim.localailab.core.navigation.destination.PlaygroundDestination

@Composable
fun rememberAppNavigationState(
    providers: Set<NavigationEntryProvider>,
    startDestination: AppDestination = PlaygroundDestination,
): AppNavigationState {
    val registry = remember(providers) { NavigationRegistry(providers) }
    check(registry.isRootDestination(startDestination)) {
        "Start destination $startDestination must be a root destination."
    }
    val startHostDestination = registry.hostDestinationFor(startDestination)
    val playgroundStack = rememberHostBackStack(
        TopLevelDestination.PLAYGROUND,
        startHostDestination,
        startDestination,
    )
    val modelsStack = rememberHostBackStack(
        TopLevelDestination.MODELS,
        startHostDestination,
        startDestination,
    )
    val runsStack = rememberHostBackStack(
        TopLevelDestination.RUNS,
        startHostDestination,
        startDestination,
    )
    val settingsStack = rememberHostBackStack(
        TopLevelDestination.SETTINGS,
        startHostDestination,
        startDestination,
    )
    val stacks = remember(playgroundStack, modelsStack, runsStack, settingsStack) {
        TopLevelBackStacks(
            playground = playgroundStack,
            models = modelsStack,
            runs = runsStack,
            settings = settingsStack,
        )
    }
    val selectedDestination = rememberSaveable(startHostDestination) {
        androidx.compose.runtime.mutableStateOf(startHostDestination)
    }

    return remember(registry, stacks, selectedDestination, startHostDestination) {
        AppNavigationState(
            registry = registry,
            stacks = stacks,
            selectedDestinationState = selectedDestination,
            startHostDestination = startHostDestination,
        )
    }
}

@Composable
private fun rememberHostBackStack(
    hostDestination: TopLevelDestination,
    startHostDestination: TopLevelDestination,
    startDestination: AppDestination,
): NavBackStack<NavKey> = rememberNavBackStack(
    *if (hostDestination == startHostDestination) {
        arrayOf(startDestination)
    } else {
        emptyArray()
    },
)

class AppNavigationState internal constructor(
    private val registry: NavigationRegistry,
    private val stacks: TopLevelBackStacks,
    private val selectedDestinationState: MutableState<TopLevelDestination>,
    private val startHostDestination: TopLevelDestination,
) : AppNavigator {
    var selectedDestination by selectedDestinationState
        private set

    val activeStack: NavBackStack<NavKey>
        get() = stacks[selectedDestination]

    val shouldHandleSystemBack: Boolean
        get() = activeStack.size == 1 && selectedDestination != startHostDestination

    val canNavigateUp: Boolean
        get() = activeStack.size > 1

    override fun navigate(destination: AppDestination) {
        val hostDestination = registry.hostDestinationFor(destination)
        val stack = stacks[hostDestination]
        val isRootDestination = registry.isRootDestination(destination)
        check(stack.isNotEmpty() || isRootDestination) {
            "Cannot navigate to child destination $destination before initializing host $hostDestination."
        }
        if (stack.isEmpty() || (!isRootDestination && stack.lastOrNull() != destination)) {
            stack.add(destination)
        }
        selectedDestination = hostDestination
    }

    override fun navigateBack() {
        if (activeStack.size > 1) {
            activeStack.removeLastOrNull()
        } else if (selectedDestination != startHostDestination) {
            selectedDestination = startHostDestination
        }
    }

    internal fun entryFor(key: NavKey): NavEntry<NavKey>? = registry.entryFor(key, this)
}

internal data class TopLevelBackStacks(
    val playground: NavBackStack<NavKey>,
    val models: NavBackStack<NavKey>,
    val runs: NavBackStack<NavKey>,
    val settings: NavBackStack<NavKey>,
) {
    operator fun get(destination: TopLevelDestination): NavBackStack<NavKey> = when (destination) {
        TopLevelDestination.PLAYGROUND -> playground
        TopLevelDestination.MODELS -> models
        TopLevelDestination.RUNS -> runs
        TopLevelDestination.SETTINGS -> settings
    }
}
