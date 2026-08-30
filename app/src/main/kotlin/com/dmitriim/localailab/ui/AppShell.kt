package com.dmitriim.localailab.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dmitriim.localailab.R
import com.dmitriim.localailab.core.navigation.AppNavHost
import com.dmitriim.localailab.core.navigation.rememberAppNavigationState
import com.dmitriim.localailab.di.AppGraph
import com.dmitriim.localailab.feature.playground.api.navigation.PlaygroundDestination
import com.dmitriim.localailab.ui.navigation.AdaptiveNavigationScaffold

@Composable
fun LocalAiLabApp(graph: AppGraph) {
    val navigationState = rememberAppNavigationState(
        providers = graph.navigationEntryProviders,
        startDestination = PlaygroundDestination,
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, graph.foregroundOperationCoordinator) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                graph.foregroundOperationCoordinator.interruptActiveOperations()
                graph.runtimeLeaseManager.evictAll()
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
        onSelectDestination = navigationState::navigate,
        onNavigateUp = if (navigationState.canNavigateUp) navigationState::navigateBack else null,
        toolbarTitle = navigationState.toolbarTitle,
    ) { modifier ->
        AppNavHost(
            state = navigationState,
            unavailableDestination = {
                Text(stringResource(R.string.destination_unavailable))
            },
            modifier = modifier,
        )
    }
}
