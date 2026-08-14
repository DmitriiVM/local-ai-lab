package com.dmitriim.localailab.feature.runs.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.core.navigation.NavigationTarget
import com.dmitriim.localailab.feature.runs.presentation.ui.RunsScreen
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
            viewModel.repeatSelected()?.let { run -> navigator.navigate(run.capability.replayTarget) }
        },
    )
}

private val com.dmitriim.localailab.core.model.capability.AiCapability.replayTarget get() = when (this) {
    com.dmitriim.localailab.core.model.capability.AiCapability.CHAT -> NavigationTarget.ASSISTANT
    com.dmitriim.localailab.core.model.capability.AiCapability.SPEECH_TO_TEXT -> NavigationTarget.SPEECH_TO_TEXT
    com.dmitriim.localailab.core.model.capability.AiCapability.TEXT_TO_SPEECH -> NavigationTarget.TEXT_TO_SPEECH
    com.dmitriim.localailab.core.model.capability.AiCapability.VOICE_ACTIVITY_DETECTION -> NavigationTarget.SPEECH_TO_TEXT
    com.dmitriim.localailab.core.model.capability.AiCapability.VOICE_ASSISTANT -> NavigationTarget.ASSISTANT
}
