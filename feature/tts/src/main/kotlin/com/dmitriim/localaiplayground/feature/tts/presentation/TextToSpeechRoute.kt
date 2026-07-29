package com.dmitriim.localaiplayground.feature.tts.presentation

import android.content.Intent
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localaiplayground.feature.tts.presentation.ui.TextToSpeechScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel
import java.io.File

@Composable
fun TextToSpeechRoute(viewModel: TextToSpeechViewModel = metroViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/wav"),
    ) { uri ->
        uri?.let(viewModel::export)
    }
    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.startReferenceRecording() else viewModel.microphonePermissionDenied()
    }
    val referencePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(viewModel::importReferenceAudio)
    }
    var consentAction by remember { mutableStateOf<ReferenceConsentAction?>(null) }

    TextToSpeechScreen(
        state = state,
        onSelectModel = viewModel::selectModel,
        onSelectVoice = viewModel::selectVoice,
        onPreviewVoice = viewModel::previewVoice,
        onRecordReference = { consentAction = ReferenceConsentAction.RECORD },
        onStopReferenceRecording = viewModel::stopReferenceRecording,
        onImportReference = { consentAction = ReferenceConsentAction.IMPORT },
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
        onPause = viewModel::pausePlayback,
        onResume = viewModel::resumePlayback,
        onStop = viewModel::stop,
        onReplay = viewModel::replay,
        onExport = { exporter.launch(state.output?.displayName ?: "local-ai-speech.wav") },
        onShare = {
            val output = state.output ?: return@TextToSpeechScreen
            runCatching {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.files",
                    File(output.filePath),
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
                viewModel.shareFailed(error.message ?: "No app is available to share this WAV file.")
            }
        },
    )
    consentAction?.let { action ->
        AlertDialog(
            onDismissRequest = { consentAction = null },
            title = { Text("Voice cloning permission") },
            text = {
                Text(
                    "Confirm that you own this voice or have the speaker’s permission to use it for voice cloning.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        consentAction = null
                        when (action) {
                            ReferenceConsentAction.RECORD ->
                                microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                            ReferenceConsentAction.IMPORT ->
                                referencePicker.launch(arrayOf("audio/*"))
                        }
                    },
                ) { Text("I confirm") }
            },
            dismissButton = {
                TextButton(onClick = { consentAction = null }) { Text("Cancel") }
            },
        )
    }
}

private enum class ReferenceConsentAction { RECORD, IMPORT }
