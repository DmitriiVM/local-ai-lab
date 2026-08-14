package com.dmitriim.localailab.feature.playground.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.core.navigation.NavigationTarget
import com.dmitriim.localailab.feature.playground.presentation.ui.PlaygroundScreen
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
    )
}

private val AiCapability.navigationTarget: NavigationTarget
    get() = when (this) {
        AiCapability.CHAT -> NavigationTarget.ASSISTANT
        AiCapability.SPEECH_TO_TEXT -> NavigationTarget.SPEECH_TO_TEXT
        AiCapability.TEXT_TO_SPEECH -> NavigationTarget.TEXT_TO_SPEECH
        AiCapability.VOICE_ACTIVITY_DETECTION -> NavigationTarget.SPEECH_TO_TEXT
        AiCapability.VOICE_ASSISTANT -> NavigationTarget.ASSISTANT
    }
