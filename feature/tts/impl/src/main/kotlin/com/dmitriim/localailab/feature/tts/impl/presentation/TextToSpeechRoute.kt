package com.dmitriim.localailab.feature.tts.impl.presentation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.feature.benchmark.api.navigation.BenchmarkDestination
import com.dmitriim.localailab.feature.tts.impl.presentation.ui.TextToSpeechActivityCallbacks
import com.dmitriim.localailab.feature.tts.impl.presentation.ui.TextToSpeechScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel
import java.io.File

@Composable
fun TextToSpeechRoute(
    navigator: AppNavigator,
    viewModel: TextToSpeechViewModel = metroViewModel(),
) {
    val title = stringResource(CoreUiR.string.tts_text_to_speech_screen_177)
    LaunchedEffect(navigator, title) {
        navigator.setToolbarTitle(title)
    }
    DisposableEffect(viewModel) {
        viewModel.runtimeLeaseController.onVisible()
        onDispose(viewModel.runtimeLeaseController::onHidden)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    TextToSpeechScreen(
        state = state,
        onSelectModel = viewModel::selectModel,
        onSelectVoice = viewModel::selectVoice,
        onPreviewVoice = viewModel::previewVoice,
        activityCallbacks = TextToSpeechActivityCallbacks(
            export = viewModel::export,
            startReferenceRecording = viewModel::startReferenceRecording,
            microphonePermissionDenied = viewModel::microphonePermissionDenied,
            importReferenceAudio = viewModel::importReferenceAudio,
        ),
        onStopReferenceRecording = viewModel::stopReferenceRecording,
        onDeleteReference = viewModel::deleteReferenceVoice,
        onTextChange = viewModel::updateText,
        onSelectLanguage = viewModel::selectLanguage,
        onApplySample = viewModel::applySample,
        onSpeedChange = viewModel::updateSpeed,
        onSentenceSilenceChange = viewModel::updateSentenceSilence,
        onVolumeChange = viewModel::updateVolume,
        onThreadCountChange = viewModel::updateThreadCount,
        onPitchChange = viewModel::updatePitch,
        onFormantChange = viewModel::updateFormant,
        onLowEqChange = viewModel::updateLowEq,
        onMidEqChange = viewModel::updateMidEq,
        onHighEqChange = viewModel::updateHighEq,
        onSaturationChange = viewModel::updateSaturation,
        onResetAudioEffects = viewModel::resetAudioEffects,
        onSynthesize = viewModel::synthesize,
        onProfile = { if (viewModel.prepareProfile()) navigator.navigate(BenchmarkDestination) },
        onPause = viewModel::pausePlayback,
        onResume = viewModel::resumePlayback,
        onStop = viewModel::stop,
        onReplay = viewModel::replay,
        onShare = {
            val output = state.output ?: return@TextToSpeechScreen
            shareGeneratedAudio(
                context = context,
                filePath = output.filePath,
                onFailure = viewModel::shareFailed,
            )
        },
    )
}

private fun shareGeneratedAudio(
    context: android.content.Context,
    filePath: String,
    onFailure: (String) -> Unit,
) {
    runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            File(filePath),
        )
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "audio/wav"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Share generated speech",
            ),
        )
    }.onFailure { error ->
        onFailure(error.message ?: "No app is available to share this WAV file.")
    }
}
