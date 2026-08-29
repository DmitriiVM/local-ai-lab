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
import com.dmitriim.localailab.core.navigation.destination.SpeechToTextDestination
import com.dmitriim.localailab.core.navigation.destination.TextToSpeechDestination
import com.dmitriim.localailab.core.navigation.rememberAppNavigationState
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.di.AppGraph
import com.dmitriim.localailab.ui.navigation.AdaptiveNavigationScaffold

@Composable
fun LocalAiLabApp(graph: AppGraph) {
    val navigationState = rememberAppNavigationState(graph.navigationEntryProviders)

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

    val toolbarTitle = when (navigationState.activeStack.lastOrNull()) {
        SpeechToTextDestination -> stringResource(CoreUiR.string.stt_speech_to_text_screen_131)
        TextToSpeechDestination -> stringResource(CoreUiR.string.tts_text_to_speech_screen_177)
        else -> null
    }

    AdaptiveNavigationScaffold(
        selectedDestination = navigationState.selectedDestination,
        showTopLevelNavigation = !navigationState.canNavigateUp,
        onSelectDestination = navigationState::selectTopLevelDestination,
        onNavigateUp = if (navigationState.canNavigateUp) navigationState::navigateBack else null,
        toolbarTitle = toolbarTitle,
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
