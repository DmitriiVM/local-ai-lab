package com.dmitriim.localaiplayground.feature.tts.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackStatus
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions
import com.dmitriim.localaiplayground.core.result.StatusMessage
import com.dmitriim.localaiplayground.feature.tts.presentation.TextToSpeechUiState
import com.dmitriim.localaiplayground.feature.tts.presentation.TtsLanguage
import com.dmitriim.localaiplayground.feature.tts.presentation.TtsOperation

@Composable
fun TextToSpeechScreen(
    state: TextToSpeechUiState,
    onSelectModel: (ModelId) -> Unit,
    onTextChange: (String) -> Unit,
    onSelectLanguage: (TtsLanguage) -> Unit,
    onApplySample: (TtsLanguage) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSentenceSilenceChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onThreadCountChange: (String) -> Unit,
    onSynthesize: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onReplay: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    val busy = state.operation != TtsOperation.IDLE ||
        state.playback.status in setOf(
            SpeechPlaybackStatus.READY,
            SpeechPlaybackStatus.PLAYING,
            SpeechPlaybackStatus.PAUSED,
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                top = dimensions.topBarOverlayClearance + 12.dp,
                end = 16.dp,
                bottom = 24.dp + dimensions.bottomNavigationOverlayClearance,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Local text to speech", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Supertonic 3 synthesizes English or Russian entirely on-device. PCM streams through one retained Android output track while a replayable WAV is prepared.",
            style = MaterialTheme.typography.bodyMedium,
        )

        TextToSpeechModelPicker(state.models, state.selectedModelId, !busy, onSelectModel)

        OutlinedTextField(
            value = state.text,
            onValueChange = onTextChange,
            enabled = !busy,
            label = { Text("Text to synthesize") },
            supportingText = { Text("${state.text.length} / ${state.characterLimit} characters") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )

        TextToSpeechLanguageControls(state.language, !busy, onSelectLanguage, onApplySample)
        TextToSpeechSettings(
            state = state,
            enabled = !busy,
            onSpeedChange = onSpeedChange,
            onSentenceSilenceChange = onSentenceSilenceChange,
            onVolumeChange = onVolumeChange,
            onThreadCountChange = onThreadCountChange,
        )
        TextToSpeechPlaybackControls(
            state = state,
            onSynthesize = onSynthesize,
            onPause = onPause,
            onResume = onResume,
            onStop = onStop,
            onReplay = onReplay,
            onExport = onExport,
            onShare = onShare,
        )

        TextToSpeechPlaybackStatus(state.playback)
        state.output?.let { GeneratedAudioCard(it) }
        state.metrics?.let { TextToSpeechMetricsCard(it) }
        state.statusMessage?.let {
            StatusMessage(title = "Text to speech", explanation = it)
        }
        state.errorMessage?.let {
            StatusMessage(title = "Text to speech needs attention", explanation = it)
        }
    }
}
