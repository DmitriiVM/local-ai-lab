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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.ui.R as CoreUiR
import com.dmitriim.localaiplayground.core.voice.tts.SpeechSynthesisMetrics

@Composable
internal fun TextToSpeechMetricsCard(metrics: SpeechSynthesisMetrics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(CoreUiR.string.tts_text_to_speech_metrics_card_175), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(
                        CoreUiR.string.ui_copy_83,
                        formatTtsDuration(metrics.synthesisDurationMs),
                        metrics.realTimeFactor.formatRealTimeFactor(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()

            TtsPlaybackMetrics(metrics)
            TtsOutputMetrics(metrics)
            TtsRuntimeMetrics(metrics)
            ChatterboxMetrics(metrics)
        }
    }
}

@Composable
private fun TtsPlaybackMetrics(metrics: SpeechSynthesisMetrics) {
    TtsMetricsSection("Response and playback")
    TtsMetricsItem(stringResource(CoreUiR.string.ui_copy_84), metrics.timeToFirstChunkMs.formatOptionalDuration(), stringResource(CoreUiR.string.ui_description_37))
    TtsMetricsItem(stringResource(CoreUiR.string.ui_copy_85), metrics.timeToFirstWriteMs.formatOptionalDuration(), stringResource(CoreUiR.string.ui_description_38))
    TtsMetricsItem(stringResource(CoreUiR.string.ui_copy_86), metrics.timeToFirstPresentationMs.formatOptionalDuration(), if (metrics.timeToFirstPresentationMs == null) "This audio route did not expose a reliable presentation timestamp, so callback time is not substituted." else "Time until Android reported that the first generated audio frame reached the output device.")
}

@Composable
private fun TtsOutputMetrics(metrics: SpeechSynthesisMetrics) {
    TtsMetricsSection("Synthesis output")
    TtsMetricsItem(stringResource(CoreUiR.string.ui_copy_87), formatTtsDuration(metrics.synthesisDurationMs), stringResource(CoreUiR.string.ui_description_39))
    TtsMetricsItem(stringResource(CoreUiR.string.ui_copy_88), formatTtsDuration(metrics.generatedAudioDurationMs), stringResource(CoreUiR.string.ui_description_40))
    TtsMetricsItem(stringResource(CoreUiR.string.ui_copy_89), metrics.realTimeFactor.formatRealTimeFactor(), stringResource(CoreUiR.string.ui_description_41))
    TtsMetricsItem(stringResource(CoreUiR.string.ui_copy_90), "${metrics.sampleRateHz} Hz · mono PCM16", stringResource(CoreUiR.string.ui_description_42))
}

@Composable
private fun TtsRuntimeMetrics(metrics: SpeechSynthesisMetrics) {
    TtsMetricsSection("Runtime")
    TtsMetricsItem(stringResource(CoreUiR.string.ui_copy_91), formatTtsDuration(metrics.loadDurationMs), stringResource(CoreUiR.string.ui_description_43))
    TtsMetricsItem(stringResource(CoreUiR.string.ui_copy_92), metrics.effectiveThreadCount.toString(), stringResource(CoreUiR.string.ui_description_44))
    TtsMetricsItem(stringResource(CoreUiR.string.ui_copy_93), metrics.playbackUnderrunCount.toString(), stringResource(CoreUiR.string.ui_description_45))
}

@Composable
private fun ChatterboxMetrics(metrics: SpeechSynthesisMetrics) {
    metrics.conditioningDurationMs?.let { duration ->
        TtsMetricsSection("Chatterbox pipeline")
        TtsMetricsItem(stringResource(CoreUiR.string.ui_copy_94), "${formatTtsDuration(duration)} · ${if (metrics.conditioningCacheHit == true) "cache hit" else "encoded"}", stringResource(CoreUiR.string.ui_description_46))
        TtsMetricsItem(stringResource(CoreUiR.string.ui_copy_95), metrics.generatedTokenCount?.toString() ?: "Unavailable", stringResource(CoreUiR.string.ui_description_47))
        TtsMetricsItem(stringResource(CoreUiR.string.ui_copy_96), metrics.tokenGenerationDurationMs.formatOptionalDuration(), stringResource(CoreUiR.string.ui_description_48))
        TtsMetricsItem(stringResource(CoreUiR.string.ui_copy_97), metrics.decoderDurationMs.formatOptionalDuration(), stringResource(CoreUiR.string.ui_description_49))
        TtsMetricsItem(stringResource(CoreUiR.string.ui_copy_98), "${metrics.peakProcessPssBytes.toMebibytes()} peak app · ${metrics.availableDeviceMemoryBytes.toMebibytes()} device available", stringResource(CoreUiR.string.ui_description_50))
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
