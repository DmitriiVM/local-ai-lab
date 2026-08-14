package com.dmitriim.localailab.feature.assistant.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.core.navigation.NavigationTarget
import com.dmitriim.localailab.feature.assistant.presentation.ui.AssistantScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun AssistantRoute(
    navigator: AppNavigator,
    viewModel: AssistantViewModel = metroViewModel(),
) {
    DisposableEffect(viewModel) {
        viewModel.runtimeLifecycle.onVisible()
        onDispose(viewModel.runtimeLifecycle::onHidden)
    }
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.startRecording() else viewModel.microphonePermissionDenied()
    }
    AssistantScreen(
        uiState = uiState,
        onUpdateInput = viewModel::updateInput,
        onSelectMode = viewModel::selectInputMode,
        onStartRecording = { microphonePermission.launch(Manifest.permission.RECORD_AUDIO) },
        onStopRecording = viewModel::stopRecording,
        onSend = viewModel::send,
        onProfile = {
            if (viewModel.prepareProfile()) navigator.navigate(NavigationTarget.BENCHMARK)
        },
        onCancel = viewModel::cancel,
        onRegenerate = viewModel::regenerate,
        onSpeakMessage = viewModel::speakMessage,
        onClearConversation = viewModel::clearConversation,
        onEditAndRetry = viewModel::editAndRetry,
        onApplyChatSettings = viewModel::applyChatSettings,
        onApplySpeechInputSettings = viewModel::applySpeechInputSettings,
        onApplySpeechOutputSettings = viewModel::applySpeechOutputSettings,
        onPreviewVoice = viewModel::previewVoice,
        onUnloadChatModel = viewModel::unloadChatModel,
        onOpenModels = { navigator.navigate(NavigationTarget.MODELS) },
    )
}
