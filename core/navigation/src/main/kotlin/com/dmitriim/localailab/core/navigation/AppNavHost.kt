package com.dmitriim.localailab.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

@Composable
fun AppNavHost(
    state: AppNavigationState,
    unavailableDestination: @Composable (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        modifier = modifier,
        backStack = state.activeStack,
        onBack = state::navigateBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
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
        entryProvider = { key ->
            state.entryFor(key) ?: NavEntry(key) {
                unavailableDestination(key)
            }
        },
    )
}

private const val NavigationTransitionDurationMillis = 180
