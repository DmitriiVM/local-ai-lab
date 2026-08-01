package com.dmitriim.localaiplayground.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dmitriim.localaiplayground.core.navigation.AppNavHost
import com.dmitriim.localaiplayground.core.navigation.rememberAppNavigationState
import com.dmitriim.localaiplayground.di.AppGraph

@Composable
fun LocalAiPlaygroundApp(graph: AppGraph) {
    val navigationState = rememberAppNavigationState(graph.navigationEntryProviders)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, graph.foregroundOperationCoordinator) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                graph.foregroundOperationCoordinator.interruptActiveOperations()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(
        enabled = navigationState.shouldHandleSystemBack,
        onBack = navigationState::navigateBack,
    )

    AdaptiveNavigationScaffold(
        selectedDestination = navigationState.selectedDestination,
        showTopLevelNavigation = !navigationState.canNavigateUp,
        onSelectDestination = navigationState::selectTopLevelDestination,
        onNavigateUp = if (navigationState.canNavigateUp) navigationState::navigateBack else null,
    ) { modifier ->
        AppNavHost(
            state = navigationState,
            unavailableDestination = {
                Text("This destination is no longer available.")
            },
            modifier = modifier,
        )
    }
}
