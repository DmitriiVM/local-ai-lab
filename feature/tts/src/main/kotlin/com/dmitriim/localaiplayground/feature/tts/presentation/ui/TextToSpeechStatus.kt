package com.dmitriim.localaiplayground.feature.tts.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.audio.output.model.GeneratedAudioFile
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackState
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackStatus
import com.dmitriim.localaiplayground.core.ui.R as CoreUiR

@Composable
internal fun TextToSpeechPlaybackStatus(playback: SpeechPlaybackState) {
    if (playback.status !in setOf(SpeechPlaybackStatus.PLAYING, SpeechPlaybackStatus.PAUSED)) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(
                    if (playback.status == SpeechPlaybackStatus.PAUSED) {
                        CoreUiR.string.tts_playback_paused
                    } else {
                        CoreUiR.string.tts_playing_generated_speech
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(CoreUiR.string.tts_text_to_speech_status_format_18, formatDuration(playback.positionMs), formatDuration(playback.queuedDurationMs)),
                fontFamily = FontFamily.Monospace,
            )
            playback.focusMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
internal fun GeneratedAudioCard(output: GeneratedAudioFile) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(CoreUiR.string.tts_text_to_speech_status_185), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(CoreUiR.string.tts_text_to_speech_status_format_19, formatDuration(output.durationMs), output.sampleRateHz))
            Text(
                stringResource(CoreUiR.string.tts_text_to_speech_status_186),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1_000
    val millis = durationMs % 1_000
    return "%d:%02d.%03d".format(seconds / 60, seconds % 60, millis)
}
