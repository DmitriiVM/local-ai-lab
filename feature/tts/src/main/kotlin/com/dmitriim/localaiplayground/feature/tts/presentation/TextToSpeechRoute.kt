package com.dmitriim.localaiplayground.feature.tts.presentation

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

    TextToSpeechScreen(
        state = state,
        onSelectModel = viewModel::selectModel,
        onTextChange = viewModel::updateText,
        onSelectLanguage = viewModel::selectLanguage,
        onApplySample = viewModel::applySample,
        onSpeedChange = viewModel::updateSpeed,
        onSentenceSilenceChange = viewModel::updateSentenceSilence,
        onVolumeChange = viewModel::updateVolume,
        onThreadCountChange = viewModel::updateThreadCount,
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
}
