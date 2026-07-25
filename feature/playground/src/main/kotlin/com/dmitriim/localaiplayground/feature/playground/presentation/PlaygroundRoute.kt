package com.dmitriim.localaiplayground.feature.playground.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localaiplayground.core.model.AiCapability
import com.dmitriim.localaiplayground.core.navigation.AppNavigator
import com.dmitriim.localaiplayground.core.navigation.NavigationTarget
import com.dmitriim.localaiplayground.feature.playground.presentation.ui.PlaygroundScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun PlaygroundRoute(
    navigator: AppNavigator,
    viewModel: PlaygroundViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PlaygroundScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onOpenCapability = { capability -> navigator.navigate(capability.navigationTarget) },
        onOpenModels = { navigator.navigate(NavigationTarget.MODELS) },
        onOpenRuns = { navigator.navigate(NavigationTarget.RUNS) },
    )
}

private val AiCapability.navigationTarget: NavigationTarget
    get() = when (this) {
        AiCapability.CHAT -> NavigationTarget.CHAT
        AiCapability.SPEECH_TO_TEXT -> NavigationTarget.SPEECH_TO_TEXT
        AiCapability.TEXT_TO_SPEECH -> NavigationTarget.TEXT_TO_SPEECH
        AiCapability.VOICE_ASSISTANT -> NavigationTarget.VOICE_ASSISTANT
    }
