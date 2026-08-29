package com.dmitriim.localailab.feature.assistant.impl.presentation.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.chat.LlmContextManagement
import com.dmitriim.localailab.ai.api.chat.LlmFinishReason
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.feature.assistant.impl.presentation.ChatMetrics
import com.dmitriim.localailab.feature.assistant.impl.presentation.ContextUsage
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
                    Text(stringResource(CoreUiR.string.assistant_assistant_metrics_card_15), fontWeight = FontWeight.Bold)
                    Text(
                        contextUsage?.let { formatContextSummary(it, metrics) }
                            ?: formatPerformanceSummary(metrics),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(stringResource(if (expanded) CoreUiR.string.core_ui_hide else CoreUiR.string.core_ui_show))
                }
            }
            if (expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ChatMetricsDetails(metrics, contextUsage)
                }
            }
        }
    }
}

@Composable
private fun ChatMetricsDetails(metrics: ChatMetrics, contextUsage: ContextUsage?) {
    HorizontalDivider()
    PerformanceMetrics(metrics)
    ContextMetrics(metrics, contextUsage)
    RuntimeMetrics(metrics)
}

@Composable
private fun PerformanceMetrics(metrics: ChatMetrics) {
    RunDetailsSection("Performance")
    RunDetailsMetric(stringResource(CoreUiR.string.ui_copy_24), metrics.timeToFirstTokenMs?.let(::formatDuration) ?: "Not reached", stringResource(CoreUiR.string.ui_description_5))
    RunDetailsMetric(
        stringResource(CoreUiR.string.ui_copy_25),
        metrics.generatedTokens?.let { buildMetricValue(it, metrics.generatedTokensPerSecond) } ?: "Token count unavailable",
        if (metrics.generatedTokens == null) "This runtime did not report generated tokens or output speed." else "Response tokens produced by the model. Tokens per second measures generation speed; higher is faster.",
    )
    RunDetailsMetric(stringResource(CoreUiR.string.ui_copy_26), formatDuration(metrics.totalDurationMs), stringResource(CoreUiR.string.ui_description_6))
}

@Composable
private fun ContextMetrics(metrics: ChatMetrics, usage: ContextUsage?) {
    RunDetailsSection("Context and request")
    usage?.let {
        RunDetailsMetric(stringResource(CoreUiR.string.ui_copy_27), formatContextDetails(it), stringResource(CoreUiR.string.ui_description_7))
        if (it.omittedMessageCount > 0) RunDetailsMetric(stringResource(CoreUiR.string.ui_copy_28), "${it.omittedMessageCount}", stringResource(CoreUiR.string.ui_description_8))
    }
    RunDetailsMetric(stringResource(CoreUiR.string.ui_copy_29), metrics.promptTokens?.let { count -> buildMetricValue(count, metrics.promptTokensPerSecond) } ?: "Token count unavailable", stringResource(CoreUiR.string.ui_description_9))
    RunDetailsMetric(stringResource(CoreUiR.string.ui_copy_30), "Temperature ${metrics.effectiveSettings.temperature} · top-K ${metrics.effectiveSettings.topK} · top-P ${metrics.effectiveSettings.topP}", stringResource(CoreUiR.string.ui_description_10))
}

@Composable
private fun RuntimeMetrics(metrics: ChatMetrics) {
    RunDetailsSection("Runtime")
    RunDetailsMetric(stringResource(CoreUiR.string.ui_copy_31), metrics.modelName, stringResource(CoreUiR.string.ui_description_11))
    val startupLabel = buildString {
        append(if (metrics.coldStart) "Cold start" else "Warm start")
        metrics.effectiveThreadCount?.let { append(" · $it threads") }
    }
    RunDetailsMetric(
        stringResource(CoreUiR.string.ui_copy_32),
        startupLabel,
        stringResource(CoreUiR.string.ui_description_12),
    )
    RunDetailsMetric(stringResource(CoreUiR.string.ui_copy_33), formatDuration(metrics.loadDurationMs), stringResource(CoreUiR.string.ui_description_13))
    RunDetailsMetric(stringResource(CoreUiR.string.ui_copy_34), metrics.finishReason.displayName(), stringResource(CoreUiR.string.ui_description_14))
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
