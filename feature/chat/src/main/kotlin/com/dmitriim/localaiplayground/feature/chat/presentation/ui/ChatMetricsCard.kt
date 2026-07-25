package com.dmitriim.localaiplayground.feature.chat.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.feature.chat.presentation.ChatMetrics
import kotlin.math.roundToInt

@Composable
internal fun ChatMetricsCard(metrics: ChatMetrics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Run metrics", fontWeight = FontWeight.Bold)
            Text("${metrics.modelName} • ${if (metrics.coldStart) "cold" else "warm"} • ${metrics.effectiveThreadCount} threads")
            Text("Load ${metrics.loadDurationMs} ms • prompt ${metrics.promptTokens} tokens${metrics.promptTokensPerSecond?.let { " (${formatRate(it)} tok/s)" } ?: ""}")
            Text("TTFT ${metrics.timeToFirstTokenMs?.let { "$it ms" } ?: "not reached"} • output ${metrics.generatedTokens} tokens${metrics.generatedTokensPerSecond?.let { " (${formatRate(it)} tok/s)" } ?: ""}")
            Text("Total ${metrics.totalDurationMs} ms • ${metrics.finishReason.name.lowercase().replace('_', ' ')}")
            Text(
                "Effective: temp ${metrics.effectiveSettings.temperature}, top-K ${metrics.effectiveSettings.topK}, top-P ${metrics.effectiveSettings.topP}, max ${metrics.effectiveSettings.maxOutputTokens}, seed ${metrics.effectiveSettings.seed}, context ${metrics.effectiveSettings.contextSize}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun formatRate(value: Double): String = "${(value * 10).roundToInt() / 10.0}"
