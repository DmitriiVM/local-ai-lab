package com.dmitriim.localaiplayground.feature.stt.presentation.ui

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
import com.dmitriim.localaiplayground.core.voice.stt.SpeechTranscriptionMetrics

@Composable
internal fun SttRunMetricsCard(
    metrics: SpeechTranscriptionMetrics,
    streamingModel: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Run metrics", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Final result in ${formatSttDuration(metrics.timeToFinalMs)} · ${metrics.realTimeFactor.formatRealTimeFactor()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()

            SttMetricsSection("Timing")
            SttMetricsItem(
                label = "Audio duration",
                value = formatSttDuration(metrics.audioDurationMs),
                description = "Length of the recording or imported audio that was transcribed.",
            )
            SttMetricsItem(
                label = "Final result",
                value = formatSttDuration(metrics.timeToFinalMs),
                description = "End-to-end time from starting the run until the completed transcript was available. This includes model loading and decoding.",
            )
            SttMetricsItem(
                label = "Engine processing",
                value = formatSttDuration(metrics.processingDurationMs),
                description = "Time reported by the speech engine while it decoded audio. This can differ from the full run time because setup and app overhead are excluded.",
            )
            SttMetricsItem(
                label = "Real-time factor",
                value = metrics.realTimeFactor.formatRealTimeFactor(),
                description = "Final-result time divided by audio duration. Below 1× means the transcript completed faster than the audio length.",
            )

            SttMetricsSection("Model and decoding")
            SttMetricsItem(
                label = "Segments",
                value = metrics.segmentCount.toString(),
                description = "Number of audio pieces processed. Longer audio may be split into bounded segments before decoding.",
            )
            SttMetricsItem(
                label = "Model load",
                value = formatSttDuration(metrics.loadDurationMs),
                description = "Time spent preparing the selected speech model before transcription began.",
            )
            SttMetricsItem(
                label = "CPU threads",
                value = metrics.effectiveThreadCount.toString(),
                description = "Effective CPU worker count used by the speech runtime for this run.",
            )
            SttMetricsItem(
                label = "Recognition mode",
                value = if (streamingModel) "Streaming-capable" else "Offline segment decoding",
                description = if (streamingModel) {
                    "The selected model supports streaming, though this screen finalizes captured audio after recording stops."
                } else {
                    "The selected model transcribes bounded audio segments after recording stops."
                },
            )
        }
    }
}

@Composable
private fun SttMetricsSection(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun SttMetricsItem(
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

private fun formatSttDuration(durationMs: Long): String {
    val seconds = durationMs / 1_000
    val millis = durationMs % 1_000
    return if (seconds == 0L) {
        "$millis ms"
    } else {
        "%d:%02d".format(seconds / 60, seconds % 60)
    }
}

private fun Double?.formatRealTimeFactor(): String = this?.let { "%.2f× real time".format(it) } ?: "Unavailable"
