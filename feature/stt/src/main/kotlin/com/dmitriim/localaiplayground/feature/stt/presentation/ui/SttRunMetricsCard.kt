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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.ui.R as CoreUiR
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
                Text(stringResource(CoreUiR.string.stt_stt_run_metrics_card_150), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(
                        CoreUiR.string.ui_copy_69,
                        formatSttDuration(metrics.timeToFinalMs),
                        metrics.realTimeFactor.formatRealTimeFactor(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()

            SttMetricsSection("Timing")
            SttMetricsItem(
                label = stringResource(CoreUiR.string.ui_copy_70),
                value = formatSttDuration(metrics.audioDurationMs),
                description = stringResource(CoreUiR.string.ui_description_30),
            )
            SttMetricsItem(
                label = stringResource(CoreUiR.string.ui_copy_71),
                value = formatSttDuration(metrics.timeToFinalMs),
                description = stringResource(CoreUiR.string.ui_description_31),
            )
            SttMetricsItem(
                label = stringResource(CoreUiR.string.ui_copy_72),
                value = formatSttDuration(metrics.processingDurationMs),
                description = stringResource(CoreUiR.string.ui_description_32),
            )
            SttMetricsItem(
                label = stringResource(CoreUiR.string.ui_copy_73),
                value = metrics.realTimeFactor.formatRealTimeFactor(),
                description = stringResource(CoreUiR.string.ui_description_33),
            )

            SttMetricsSection("Model and decoding")
            SttMetricsItem(
                label = stringResource(CoreUiR.string.ui_copy_74),
                value = metrics.segmentCount.toString(),
                description = stringResource(CoreUiR.string.ui_description_34),
            )
            SttMetricsItem(
                label = stringResource(CoreUiR.string.ui_copy_75),
                value = formatSttDuration(metrics.loadDurationMs),
                description = stringResource(CoreUiR.string.ui_description_35),
            )
            SttMetricsItem(
                label = stringResource(CoreUiR.string.ui_copy_76),
                value = metrics.effectiveThreadCount.toString(),
                description = stringResource(CoreUiR.string.ui_description_36),
            )
            SttMetricsItem(
                label = stringResource(CoreUiR.string.ui_copy_77),
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
