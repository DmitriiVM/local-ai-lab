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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.audio.output.model.GeneratedAudioFile
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackState
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackStatus
import com.dmitriim.localaiplayground.feature.tts.domain.SpeechSynthesisMetrics

@Composable
internal fun TextToSpeechPlaybackStatus(playback: SpeechPlaybackState) {
    if (playback.status !in setOf(SpeechPlaybackStatus.PLAYING, SpeechPlaybackStatus.PAUSED)) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (playback.status == SpeechPlaybackStatus.PAUSED) "Playback paused" else "Playing generated speech",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "${formatDuration(playback.positionMs)} / ${formatDuration(playback.queuedDurationMs)} queued",
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
            Text("Latest generated WAV", style = MaterialTheme.typography.titleMedium)
            Text("${formatDuration(output.durationMs)} · ${output.sampleRateHz} Hz · mono PCM16")
            Text(
                "Only this latest successful synthesis is retained. It is replaced after the next successful synthesis; explicit exports are unaffected.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
internal fun TextToSpeechMetricsCard(metrics: SpeechSynthesisMetrics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Run metrics", style = MaterialTheme.typography.titleMedium)
            Text(
                "First PCM chunk: ${formatOptionalDuration(metrics.timeToFirstChunkMs)} · " +
                    "first AudioTrack write: ${formatOptionalDuration(metrics.timeToFirstWriteMs)}",
            )
            Text("First Android presentation: ${formatOptionalDuration(metrics.timeToFirstPresentationMs)}")
            Text(
                "Synthesis: ${formatDuration(metrics.synthesisDurationMs)} · " +
                    "audio: ${formatDuration(metrics.generatedAudioDurationMs)} · " +
                    "RTF: ${metrics.realTimeFactor?.let { "%.3f".format(it) } ?: "—"}",
            )
            Text(
                "${metrics.sampleRateHz} Hz · underruns: ${metrics.playbackUnderrunCount} · " +
                    "load: ${formatDuration(metrics.loadDurationMs)} · threads: ${metrics.effectiveThreadCount}",
            )
            if (metrics.conditioningDurationMs != null) {
                Text(
                    "Chatterbox conditioning: ${formatDuration(metrics.conditioningDurationMs)} " +
                        "(${if (metrics.conditioningCacheHit == true) "cache hit" else "encoded"}) · " +
                        "tokens: ${formatOptionalDuration(metrics.tokenGenerationDurationMs)} · " +
                        "decoder: ${formatOptionalDuration(metrics.decoderDurationMs)}",
                )
                Text("Generated speech tokens: ${metrics.generatedTokenCount ?: "—"}")
                Text(
                    "Peak process PSS: ${metrics.peakProcessPssBytes?.toMebibytes() ?: "—"} · " +
                        "device available: ${metrics.availableDeviceMemoryBytes?.toMebibytes() ?: "—"}",
                )
            }
            if (metrics.timeToFirstPresentationMs == null) {
                Text(
                    "This audio route did not expose a reliable presentation timestamp; callback time is not substituted.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun formatOptionalDuration(durationMs: Long?): String = durationMs?.let { "$it ms" } ?: "unavailable"

private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1_000
    val millis = durationMs % 1_000
    return "%d:%02d.%03d".format(seconds / 60, seconds % 60, millis)
}

private fun Long.toMebibytes(): String = "%.1f MiB".format(toDouble() / 1_048_576)
