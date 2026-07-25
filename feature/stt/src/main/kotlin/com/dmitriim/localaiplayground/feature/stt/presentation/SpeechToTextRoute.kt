package com.dmitriim.localaiplayground.feature.stt.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localaiplayground.feature.stt.presentation.ui.SpeechToTextScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun SpeechToTextRoute(viewModel: SpeechToTextViewModel = metroViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startRecording() else viewModel.microphonePermissionDenied()
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importAudio)
    }
    SpeechToTextScreen(
        state = state,
        onSelectModel = viewModel::selectModel,
        onSelectLanguage = viewModel::selectLanguage,
        onThreadCountChange = viewModel::updateThreadCount,
        onStartRecording = { microphonePermission.launch(Manifest.permission.RECORD_AUDIO) },
        onStopRecording = viewModel::stopRecording,
        onImportAudio = { audioPicker.launch(arrayOf("audio/*")) },
        onRepeat = viewModel::repeatTranscription,
        onCancel = viewModel::cancel,
        onClear = viewModel::clear,
    )
}
