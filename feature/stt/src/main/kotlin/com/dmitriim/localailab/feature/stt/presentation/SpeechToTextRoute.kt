package com.dmitriim.localailab.feature.stt.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.core.navigation.NavigationTarget
import com.dmitriim.localailab.feature.stt.presentation.ui.SpeechToTextScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun SpeechToTextRoute(
    navigator: AppNavigator,
    viewModel: SpeechToTextViewModel = metroViewModel(),
) {
    DisposableEffect(viewModel) {
        viewModel.runtimeLifecycle.onVisible()
        onDispose(viewModel.runtimeLifecycle::onHidden)
    }
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
        onProfile = {
            if (viewModel.prepareProfile()) navigator.navigate(NavigationTarget.BENCHMARK)
        },
        onCancel = viewModel::cancel,
        onClear = viewModel::clear,
    )
}
