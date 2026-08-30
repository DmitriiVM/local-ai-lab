package com.dmitriim.localailab.feature.stt.impl.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.feature.benchmark.api.navigation.BenchmarkDestination
import com.dmitriim.localailab.feature.stt.impl.presentation.ui.SpeechToTextScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun SpeechToTextRoute(
    navigator: AppNavigator,
    viewModel: SpeechToTextViewModel = metroViewModel(),
) {
    val title = stringResource(CoreUiR.string.stt_speech_to_text_screen_131)
    LaunchedEffect(navigator, title) {
        navigator.setToolbarTitle(title)
    }
    DisposableEffect(viewModel) {
        viewModel.runtimeLeaseController.onVisible()
        onDispose(viewModel.runtimeLeaseController::onHidden)
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
            if (viewModel.prepareProfile()) navigator.navigate(BenchmarkDestination)
        },
        onCancel = viewModel::cancel,
        onClear = viewModel::clear,
    )
}
