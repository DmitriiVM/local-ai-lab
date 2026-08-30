package com.dmitriim.localailab.feature.runs.impl.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localailab.core.navigation.AppDestination
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.feature.assistant.api.navigation.AssistantDestination
import com.dmitriim.localailab.feature.runs.impl.presentation.ui.RunsScreen
import com.dmitriim.localailab.feature.stt.api.navigation.SpeechToTextDestination
import com.dmitriim.localailab.feature.tts.api.navigation.TextToSpeechDestination
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun RunsRoute(navigator: AppNavigator, viewModel: RunsViewModel = metroViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareUri = state.pendingShareUri
    LaunchedEffect(shareUri) {
        shareUri ?: return@LaunchedEffect
        runCatching {
            context.startActivity(
                android.content.Intent.createChooser(
                    android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(android.content.Intent.EXTRA_STREAM, shareUri.toUri())
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Share local AI run",
                ),
            )
        }
        viewModel.consumeShare()
    }
    RunsScreen(
        state = state,
        onCapabilityFilter = viewModel::setCapabilityFilter,
        onStatusFilter = viewModel::setStatusFilter,
        onSelectRun = viewModel::selectRun,
        onCloseDetails = viewModel::closeDetails,
        onRequestClearRunHistory = viewModel::requestClearRunHistory,
        onDismissClearRunHistory = viewModel::dismissClearRunHistory,
        onClearRunHistory = viewModel::clearRunHistory,
        onShare = viewModel::prepareShare,
        onRepeat = {
            viewModel.repeatSelected()?.let { run -> navigator.navigate(run.capability.replayDestination) }
        },
    )
}

private val com.dmitriim.localailab.ai.api.capability.AiCapability.replayDestination: AppDestination get() = when (this) {
    com.dmitriim.localailab.ai.api.capability.AiCapability.CHAT -> AssistantDestination
    com.dmitriim.localailab.ai.api.capability.AiCapability.SPEECH_TO_TEXT -> SpeechToTextDestination
    com.dmitriim.localailab.ai.api.capability.AiCapability.TEXT_TO_SPEECH -> TextToSpeechDestination
    com.dmitriim.localailab.ai.api.capability.AiCapability.VOICE_ACTIVITY_DETECTION -> SpeechToTextDestination
    com.dmitriim.localailab.ai.api.capability.AiCapability.VOICE_ASSISTANT -> AssistantDestination
}
