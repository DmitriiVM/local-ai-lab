package com.dmitriim.localaiplayground.feature.voice.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.feature.voice.domain.VoicePipelineMetrics

@Composable
internal fun VoiceMetricsCard(metrics: VoicePipelineMetrics) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Latency timeline", style = MaterialTheme.typography.titleMedium)
            Text("Listening: ${formatMetricDuration(metrics.listeningDurationMs)} · speech finalization: ${formatMetricDuration(metrics.speechFinalizationDurationMs)}")
            Text("STT processing: ${formatMetricDuration(metrics.sttProcessingDurationMs)}")
            Text("LLM TTFT: ${metrics.llmTimeToFirstTokenMs?.let(::formatMetricDuration) ?: "—"} · completion: ${formatMetricDuration(metrics.llmCompletionDurationMs)}")
            Text("TTS first chunk: ${metrics.ttsTimeToFirstChunkMs?.let(::formatMetricDuration) ?: "—"} · first write: ${metrics.ttsTimeToFirstWriteMs?.let(::formatMetricDuration) ?: "—"}")
            Text("First presentation: ${metrics.ttsTimeToFirstPresentationMs?.let(::formatMetricDuration) ?: "unavailable"} · completion: ${formatMetricDuration(metrics.ttsCompletionDurationMs)}")
            Text("End to first output: ${metrics.endToEndTimeToFirstOutputMs?.let(::formatMetricDuration) ?: "unavailable"}")
            Text(
                "In-memory component IDs: STT ${metrics.componentRunIds.stt.take(8)} · LLM ${metrics.componentRunIds.llm.take(8)} · TTS ${metrics.componentRunIds.tts.take(8)}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun formatMetricDuration(durationMs: Long): String = "%.2fs".format(durationMs / 1_000.0)
