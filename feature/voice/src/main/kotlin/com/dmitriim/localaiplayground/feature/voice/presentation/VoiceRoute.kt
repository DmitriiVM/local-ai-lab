package com.dmitriim.localaiplayground.feature.voice.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localaiplayground.feature.voice.presentation.ui.VoiceScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun VoiceRoute(viewModel: VoiceViewModel = metroViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startListening() else viewModel.microphonePermissionDenied()
    }
    VoiceScreen(
        state = state,
        onSelectSpeechModel = viewModel::selectSpeechModel,
        onSelectChatModel = viewModel::selectChatModel,
        onSelectVoiceModel = viewModel::selectVoiceModel,
        onSelectLanguage = viewModel::selectLanguage,
        onUpdateSettings = viewModel::updateSettings,
        onStartListening = { microphonePermission.launch(Manifest.permission.RECORD_AUDIO) },
        onStopListening = viewModel::stopListening,
        onCancel = viewModel::cancel,
        onNewConversation = viewModel::newConversation,
    )
}
