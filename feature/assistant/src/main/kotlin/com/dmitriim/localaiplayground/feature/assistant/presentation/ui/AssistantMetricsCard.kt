package com.dmitriim.localaiplayground.feature.assistant.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.ai.api.llm.LlmContextManagement
import com.dmitriim.localaiplayground.ai.api.llm.LlmFinishReason
import com.dmitriim.localaiplayground.feature.assistant.presentation.ChatMetrics
import com.dmitriim.localaiplayground.feature.assistant.presentation.ContextUsage
import kotlin.math.roundToInt

@Composable
internal fun ChatMetricsCard(
    metrics: ChatMetrics,
    contextUsage: ContextUsage?,
) {
    var expanded by remember(metrics) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Run details", fontWeight = FontWeight.Bold)
                    Text(
                        contextUsage?.let { formatContextSummary(it, metrics) }
                            ?: formatPerformanceSummary(metrics),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Show")
                }
            }
            if (expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    HorizontalDivider()

                    RunDetailsSection("Performance")
                    RunDetailsMetric(
                        label = "Time to first token",
                        value = metrics.timeToFirstTokenMs?.let(::formatDuration) ?: "Not reached",
                        description = "Time from starting the run until the first response token arrives. Lower feels more responsive.",
                    )
                    RunDetailsMetric(
                        label = "Output",
                        value = metrics.generatedTokens?.let { tokens ->
                            buildMetricValue(tokens, metrics.generatedTokensPerSecond)
                        } ?: "Token count unavailable",
                        description = if (metrics.generatedTokens == null) {
                            "This runtime did not report generated tokens or output speed."
                        } else {
                            "Response tokens produced by the model. Tokens per second measures generation speed; higher is faster."
                        },
                    )
                    RunDetailsMetric(
                        label = "Total run time",
                        value = formatDuration(metrics.totalDurationMs),
                        description = "End-to-end time for this run, including model loading, prompt processing, and generation.",
                    )

                    RunDetailsSection("Context and request")
                    contextUsage?.let { usage ->
                        RunDetailsMetric(
                            label = "Context window",
                            value = formatContextDetails(usage),
                            description = "The conversation input and reserved response space available to this run. Older messages may be omitted when the limit is reached.",
                        )
                        if (usage.omittedMessageCount > 0) {
                            RunDetailsMetric(
                                label = "Earlier messages omitted",
                                value = "${usage.omittedMessageCount}",
                                description = "Earlier conversation messages left out to keep the prompt within the context window.",
                            )
                        }
                    }
                    RunDetailsMetric(
                        label = "Prompt processing",
                        value = metrics.promptTokens?.let { tokens ->
                            buildMetricValue(tokens, metrics.promptTokensPerSecond)
                        } ?: "Token count unavailable",
                        description = "Input tokens read before the response. Tokens per second measures prompt processing speed; higher is faster.",
                    )
                    RunDetailsMetric(
                        label = "Generation settings",
                        value = "Temperature ${metrics.effectiveSettings.temperature} · top-K ${metrics.effectiveSettings.topK} · top-P ${metrics.effectiveSettings.topP}",
                        description = "Sampling controls that shape how varied the response can be. These settings affect output quality, not just speed.",
                    )

                    RunDetailsSection("Runtime")
                    RunDetailsMetric(
                        label = "Model",
                        value = metrics.modelName,
                        description = "The local model used to produce this response.",
                    )
                    RunDetailsMetric(
                        label = "Startup",
                        value = buildString {
                            append(if (metrics.coldStart) "Cold start" else "Warm start")
                            metrics.effectiveThreadCount?.let { append(" · $it threads") }
                        },
                        description = "Cold starts load a model into memory. Warm starts reuse one that is already loaded; thread count is the effective CPU parallelism.",
                    )
                    RunDetailsMetric(
                        label = "Model load",
                        value = formatDuration(metrics.loadDurationMs),
                        description = "Time spent preparing the selected model before prompt processing begins.",
                    )
                    RunDetailsMetric(
                        label = "Finish reason",
                        value = metrics.finishReason.displayName(),
                        description = "Why generation stopped for this response.",
                    )
                }
            }
        }
    }
}

private fun formatContextSummary(
    usage: ContextUsage,
    metrics: ChatMetrics,
): String = buildString {
    append("Context: ")
    usage.promptTokens?.let { tokens ->
        append(if (usage.promptTokensEstimated) "~$tokens" else tokens)
        usage.contextSize?.let { append(" / $it") }
    } ?: usage.contextSize?.let { append(it) } ?: append("unavailable")
    usage.reservedOutputTokens?.let { append(" · Output cap: $it") }
    append(" · ${formatDuration(metrics.totalDurationMs)} total")
}

private fun formatContextDetails(usage: ContextUsage): String = buildString {
    val details = buildList {
        if (usage.contextManagement == LlmContextManagement.RUNTIME_MANAGED) {
            add("Runtime managed")
        }
        usage.promptTokens?.let { tokens ->
            add(if (usage.promptTokensEstimated) "~$tokens estimated input" else "$tokens input")
        }
        usage.reservedOutputTokens?.let { add("$it max output") }
        usage.contextSize?.let { add("$it total") }
    }
    append(details.joinToString(" · ").ifEmpty { "Unavailable" })
}

private fun formatPerformanceSummary(metrics: ChatMetrics): String = buildString {
    metrics.generatedTokens?.let { append("$it\u00A0tokens") }
        ?: append("Token count unavailable")
    metrics.generatedTokensPerSecond?.let {
        append(" · ${formatRate(it)}\u00A0tok/s")
    }
    append(" · ${formatDuration(metrics.totalDurationMs)} total")
}

@Composable
private fun RunDetailsSection(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun RunDetailsMetric(
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

private fun LlmFinishReason.displayName(): String = name
    .lowercase()
    .replace('_', ' ')
    .replaceFirstChar(Char::uppercase)
