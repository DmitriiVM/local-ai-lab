package com.dmitriim.localaiplayground.feature.tts.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.voice.tts.SpeechSynthesisMetrics

@Composable
internal fun TextToSpeechMetricsCard(metrics: SpeechSynthesisMetrics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Run metrics", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Synthesis ${formatTtsDuration(metrics.synthesisDurationMs)} · ${metrics.realTimeFactor.formatRealTimeFactor()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()

            TtsMetricsSection("Response and playback")
            TtsMetricsItem(
                label = "First PCM chunk",
                value = metrics.timeToFirstChunkMs.formatOptionalDuration(),
                description = "Time until the model produced the first audio samples. Lower values make speech feel more responsive.",
            )
            TtsMetricsItem(
                label = "First playback write",
                value = metrics.timeToFirstWriteMs.formatOptionalDuration(),
                description = "Time until the app handed the first generated audio samples to Android's playback buffer.",
            )
            TtsMetricsItem(
                label = "First audible presentation",
                value = metrics.timeToFirstPresentationMs.formatOptionalDuration(),
                description = if (metrics.timeToFirstPresentationMs == null) {
                    "This audio route did not expose a reliable presentation timestamp, so callback time is not substituted."
                } else {
                    "Time until Android reported that the first generated audio frame reached the output device."
                },
            )

            TtsMetricsSection("Synthesis output")
            TtsMetricsItem(
                label = "Synthesis time",
                value = formatTtsDuration(metrics.synthesisDurationMs),
                description = "Time spent generating speech audio, excluding model loading and playback drain time.",
            )
            TtsMetricsItem(
                label = "Generated audio",
                value = formatTtsDuration(metrics.generatedAudioDurationMs),
                description = "Duration of the WAV audio created for this request.",
            )
            TtsMetricsItem(
                label = "Real-time factor",
                value = metrics.realTimeFactor.formatRealTimeFactor(),
                description = "Synthesis time divided by generated audio duration. Below 1× means audio was generated faster than it plays.",
            )
            TtsMetricsItem(
                label = "Audio format",
                value = "${metrics.sampleRateHz} Hz · mono PCM16",
                description = "Sample rate and PCM format of the generated WAV file.",
            )

            TtsMetricsSection("Runtime")
            TtsMetricsItem(
                label = "Model load",
                value = formatTtsDuration(metrics.loadDurationMs),
                description = "Time spent preparing the selected speech model before synthesis began.",
            )
            TtsMetricsItem(
                label = "CPU threads",
                value = metrics.effectiveThreadCount.toString(),
                description = "Effective CPU worker count used by the speech runtime for this run.",
            )
            TtsMetricsItem(
                label = "Playback underruns",
                value = metrics.playbackUnderrunCount.toString(),
                description = "Times Android's playback buffer ran short of audio. Zero indicates uninterrupted playback.",
            )

            metrics.conditioningDurationMs?.let { conditioningDurationMs ->
                TtsMetricsSection("Chatterbox pipeline")
                TtsMetricsItem(
                    label = "Voice conditioning",
                    value = "${formatTtsDuration(conditioningDurationMs)} · ${if (metrics.conditioningCacheHit == true) "cache hit" else "encoded"}",
                    description = "Time spent preparing the voice-conditioning representation. A cache hit reuses a previously prepared representation.",
                )
                TtsMetricsItem(
                    label = "Speech tokens",
                    value = metrics.generatedTokenCount?.toString() ?: "Unavailable",
                    description = "Intermediate speech tokens generated before waveform decoding.",
                )
                TtsMetricsItem(
                    label = "Token generation",
                    value = metrics.tokenGenerationDurationMs.formatOptionalDuration(),
                    description = "Time spent producing intermediate speech tokens.",
                )
                TtsMetricsItem(
                    label = "Waveform decoding",
                    value = metrics.decoderDurationMs.formatOptionalDuration(),
                    description = "Time spent converting generated speech tokens into PCM audio samples.",
                )
                TtsMetricsItem(
                    label = "Memory snapshot",
                    value = "${metrics.peakProcessPssBytes.toMebibytes()} peak app · ${metrics.availableDeviceMemoryBytes.toMebibytes()} device available",
                    description = "Memory readings captured by the speech runtime. PSS estimates the app's proportional share of memory.",
                )
            }
        }
    }
}

@Composable
private fun TtsMetricsSection(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun TtsMetricsItem(
    label: String,
    value: String,
    description: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Long?.formatOptionalDuration(): String = this?.let(::formatTtsDuration) ?: "Unavailable"

private fun formatTtsDuration(durationMs: Long): String {
    val seconds = durationMs / 1_000
    val millis = durationMs % 1_000
    return if (seconds == 0L) "$millis ms" else "%d:%02d.%03d".format(seconds / 60, seconds % 60, millis)
}

private fun Double?.formatRealTimeFactor(): String = this?.let { "%.3f× real time".format(it) } ?: "Unavailable"

private fun Long?.toMebibytes(): String = this?.let { "%.1f MiB".format(it / 1_048_576.0) } ?: "Unavailable"
