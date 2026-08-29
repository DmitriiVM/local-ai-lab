package com.dmitriim.localailab.feature.playground.impl.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.core.navigation.AppDestination
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.feature.assistant.api.navigation.AssistantDestination
import com.dmitriim.localailab.feature.stt.api.navigation.SpeechToTextDestination
import com.dmitriim.localailab.feature.tts.api.navigation.TextToSpeechDestination
import com.dmitriim.localailab.feature.playground.impl.presentation.ui.PlaygroundScreen
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
        onOpenCapability = { capability -> navigator.navigate(capability.destination) },
    )
}

private val AiCapability.destination: AppDestination
    get() = when (this) {
        AiCapability.CHAT -> AssistantDestination
        AiCapability.SPEECH_TO_TEXT -> SpeechToTextDestination
        AiCapability.TEXT_TO_SPEECH -> TextToSpeechDestination
        AiCapability.VOICE_ACTIVITY_DETECTION -> SpeechToTextDestination
        AiCapability.VOICE_ASSISTANT -> AssistantDestination
    }
