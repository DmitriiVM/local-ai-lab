package com.dmitriim.localaiplayground.feature.tts.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackStatus
import com.dmitriim.localaiplayground.core.ui.R as CoreUiR
import com.dmitriim.localaiplayground.feature.tts.presentation.TextToSpeechUiState
import com.dmitriim.localaiplayground.feature.tts.presentation.TtsOperation

@Composable
internal fun TextToSpeechPlaybackControls(
    state: TextToSpeechUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onReplay: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            when {
                state.operation == TtsOperation.PREVIEWING -> Unit
                state.operation in setOf(TtsOperation.SYNTHESIZING, TtsOperation.CANCELLING) -> Button(onClick = onStop, enabled = state.operation != TtsOperation.CANCELLING) { Text(stringResource(if (state.operation == TtsOperation.CANCELLING) CoreUiR.string.tts_stopping else CoreUiR.string.tts_stop)) }
                state.playback.status == SpeechPlaybackStatus.PLAYING -> {
                    Button(onClick = onPause) { Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_168)) }
                    OutlinedButton(onClick = onStop) { Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_169)) }
                }
                state.playback.status == SpeechPlaybackStatus.PAUSED -> {
                    Button(onClick = onResume) { Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_170)) }
                    OutlinedButton(onClick = onStop) { Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_171)) }
                }
                else -> if (state.output != null) OutlinedButton(onClick = onReplay) { Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_172)) }
            }
        }
        if (state.output != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onExport) { Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_173)) }
                OutlinedButton(onClick = onShare) { Text(stringResource(CoreUiR.string.tts_text_to_speech_controls_174)) }
            }
        }
    }
}
