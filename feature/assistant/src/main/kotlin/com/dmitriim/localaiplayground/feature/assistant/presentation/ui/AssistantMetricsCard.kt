package com.dmitriim.localaiplayground.feature.assistant.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.feature.assistant.presentation.ChatMetrics
import kotlin.math.roundToInt

@Composable
internal fun ChatMetricsCard(metrics: ChatMetrics) {
    var expanded by remember(metrics) { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Run details", fontWeight = FontWeight.Bold)
                    Text(
                        buildString {
                            metrics.generatedTokens?.let { append("$it\u00A0tokens") }
                                ?: append("Token count unavailable")
                            metrics.generatedTokensPerSecond?.let {
                                append(" · ${formatRate(it)}\u00A0tok/s")
                            }
                            append(" · ${formatDuration(metrics.totalDurationMs)} total")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Show")
                }
            }
            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetricRow("Model", metrics.modelName)
                    MetricRow(
                        "Startup",
                        buildString {
                            append(if (metrics.coldStart) "Cold" else "Warm")
                            metrics.effectiveThreadCount?.let { append(" · $it threads") }
                        },
                    )
                    MetricRow("Load", "${metrics.loadDurationMs}\u00A0ms")
                    metrics.promptTokens?.let { tokens ->
                        MetricRow("Prompt", buildMetricValue(tokens, metrics.promptTokensPerSecond))
                    }
                    MetricRow(
                        "First token",
                        metrics.timeToFirstTokenMs?.let { "$it\u00A0ms" } ?: "Not reached",
                    )
                    metrics.generatedTokens?.let { tokens ->
                        MetricRow("Output", buildMetricValue(tokens, metrics.generatedTokensPerSecond))
                    }
                    MetricRow("Total", formatDuration(metrics.totalDurationMs))
                    MetricRow(
                        "Finish",
                        metrics.finishReason.name
                            .lowercase()
                            .replace('_', ' ')
                            .replaceFirstChar(Char::uppercase),
                    )
                    Text(
                        "Generation: temperature ${metrics.effectiveSettings.temperature} · " +
                            "top-K ${metrics.effectiveSettings.topK} · top-P ${metrics.effectiveSettings.topP}\n" +
                            "Limits: ${metrics.effectiveSettings.maxOutputTokens} output · " +
                            "${metrics.effectiveSettings.contextSize} context · seed " +
                            (metrics.effectiveSettings.seed?.toString() ?: "engine-selected"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.weight(0.38f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.62f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End,
        )
    }
}

private fun buildMetricValue(tokens: Int, rate: Double?): String = buildString {
    append("$tokens\u00A0tokens")
    rate?.let { append(" · ${formatRate(it)}\u00A0tok/s") }
}

private fun formatDuration(milliseconds: Long): String = if (milliseconds < 1_000) {
    "$milliseconds\u00A0ms"
} else {
    "${(milliseconds / 100.0).roundToInt() / 10.0}\u00A0s"
}

private fun formatRate(value: Double): String = "${(value * 10).roundToInt() / 10.0}"
