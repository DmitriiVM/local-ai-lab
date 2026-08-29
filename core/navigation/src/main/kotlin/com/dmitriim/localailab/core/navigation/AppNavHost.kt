package com.dmitriim.localailab.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.ui.NavDisplay

@Composable
fun AppNavHost(
    state: AppNavigationState,
    unavailableDestination: @Composable (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playgroundEntries = rememberHostEntries(
        state,
        TopLevelDestination.PLAYGROUND,
        unavailableDestination,
    )
    val modelsEntries = rememberHostEntries(
        state,
        TopLevelDestination.MODELS,
        unavailableDestination,
    )
    val runsEntries = rememberHostEntries(
        state,
        TopLevelDestination.RUNS,
        unavailableDestination,
    )
    val settingsEntries = rememberHostEntries(
        state,
        TopLevelDestination.SETTINGS,
        unavailableDestination,
    )
    val activeEntries = when (state.selectedDestination) {
        TopLevelDestination.PLAYGROUND -> playgroundEntries
        TopLevelDestination.MODELS -> modelsEntries
        TopLevelDestination.RUNS -> runsEntries
        TopLevelDestination.SETTINGS -> settingsEntries
    }

    NavDisplay(
        modifier = modifier,
        entries = activeEntries,
        onBack = state::navigateBack,
        transitionSpec = {
            fadeIn(animationSpec = tween(NavigationTransitionDurationMillis)) togetherWith
                fadeOut(animationSpec = tween(NavigationTransitionDurationMillis))
        },
        popTransitionSpec = {
            fadeIn(animationSpec = tween(NavigationTransitionDurationMillis)) togetherWith
                fadeOut(animationSpec = tween(NavigationTransitionDurationMillis))
        },
        predictivePopTransitionSpec = { _ ->
            fadeIn(animationSpec = tween(NavigationTransitionDurationMillis)) togetherWith
                fadeOut(animationSpec = tween(NavigationTransitionDurationMillis))
        },
    )
}

@Composable
private fun rememberHostEntries(
    state: AppNavigationState,
    destination: TopLevelDestination,
    unavailableDestination: @Composable (NavKey) -> Unit,
): List<NavEntry<NavKey>> = rememberDecoratedNavEntries(
    backStack = state.stackFor(destination),
    entryDecorators = state.entryDecoratorsFor(destination),
    entryProvider = { key ->
        state.entryFor(key) ?: NavEntry(key) {
            unavailableDestination(key)
        }
    },
)

private const val NavigationTransitionDurationMillis = 180
