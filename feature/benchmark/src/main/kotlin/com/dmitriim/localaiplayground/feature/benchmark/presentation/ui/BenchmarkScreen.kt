package com.dmitriim.localaiplayground.feature.benchmark.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.performance.BenchmarkIterationResult
import com.dmitriim.localaiplayground.core.performance.BenchmarkWorkload
import com.dmitriim.localaiplayground.core.performance.InferenceResourceMetrics
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions
import com.dmitriim.localaiplayground.feature.benchmark.presentation.BenchmarkLabUiState

@Composable
fun BenchmarkScreen(
    state: BenchmarkLabUiState,
    onWarmupsChange: (Int) -> Unit,
    onMeasuredChange: (Int) -> Unit,
    onToggleStartupMode: () -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onToggleComparison: (String) -> Unit,
    onCompare: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    val workload = state.workload
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
            start = 20.dp,
            top = dimensions.topBarOverlayClearance + 20.dp,
            end = 20.dp,
            bottom = 44.dp + dimensions.bottomNavigationOverlayClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)
        if (workload == null) {
            Text("Open Profile from a Chat, STT, or TTS screen so its selected model and current workload can be used.")
            return@Column
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(workload.capabilityLabel, style = MaterialTheme.typography.titleMedium)
                Text("Model: ${workload.modelDisplayName}")
                Text(workload.workloadDescription, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Model and workload come from the originating screen and cannot be changed here.", style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Profile configuration", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onWarmupsChange(state.warmupIterations - 1) }, enabled = !state.isRunning) { Text("−") }
                    Text("Warm-ups: ${state.warmupIterations}", Modifier.padding(top = 12.dp))
                    TextButton(onClick = { onWarmupsChange(state.warmupIterations + 1) }, enabled = !state.isRunning) { Text("+") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onMeasuredChange(state.measuredIterations - 1) }, enabled = !state.isRunning) { Text("−") }
                    Text("Measured: ${state.measuredIterations}", Modifier.padding(top = 12.dp))
                    TextButton(onClick = { onMeasuredChange(state.measuredIterations + 1) }, enabled = !state.isRunning) { Text("+") }
                }
                TextButton(onClick = onToggleStartupMode, enabled = !state.isRunning) { Text("Startup: ${state.startupMode.name.lowercase().replaceFirstChar(Char::titlecase)}") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStart, enabled = !state.isRunning) { Text("Start profiling") }
            if (state.isRunning) Button(onClick = onCancel) { Text("Cancel") }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
        if (state.completedIterations.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Measured results", style = MaterialTheme.typography.titleMedium)
                    Text(if (state.completedIterations.any { it.telemetry.traceActive }) "External trace active." else "No external trace detected; in-app telemetry is still collected.")
                    state.completedIterations.forEach { result ->
                        Text("#${result.iteration}: ${result.latencyMs} ms · ${result.performanceText(workload.capability)}")
                    }
                    state.completedIterations.last().telemetry.resources?.let { resources ->
                        Text("CPU avg/peak: ${resources.averageProcessCpuPercent?.let { "%.1f".format(it) } ?: "—"}/${resources.peakProcessCpuPercent?.let { "%.1f".format(it) } ?: "—"}%")
                        Text("Peak PSS: ${resources.peakPssBytes?.let { "%.1f".format(it / 1_048_576.0) } ?: "—"} MiB · thermal: ${resources.thermalStatusEnd ?: "—"}")
                        Text(resources.batteryText())
                    }
                    state.summary?.let {
                        Text("Median ${it.medianLatencyMs ?: "—"} ms · p95 ${it.p95LatencyMs ?: "—"} ms · min/max ${it.minimumLatencyMs ?: "—"}/${it.maximumLatencyMs ?: "—"} ms")
                        it.totalBatteryEnergyDeltaNwh?.let { energy -> Text("Session ${energy.energyText()}") }
                    }
                }
            }
        }
        val compatibleSessions = state.savedSessions.filter { it.capability == workload.capability }
        if (compatibleSessions.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("History & compare", style = MaterialTheme.typography.titleMedium)
                    compatibleSessions.forEach { session ->
                        TextButton(onClick = { onToggleComparison(session.id) }) {
                            Text("${if (session.id in state.compareSessionIds) "✓ " else ""}${session.model?.displayName ?: "Unknown model"} · ${session.status.name.lowercase()}")
                        }
                    }
                    Button(onClick = onCompare, enabled = state.compareSessionIds.size == 2) { Text("Compare selected") }
                    state.comparison?.let { Text(it) }
                }
            }
        }
    }
}

private val BenchmarkWorkload.capability: AiCapability
    get() = when (this) {
        is BenchmarkWorkload.Chat -> AiCapability.CHAT
        is BenchmarkWorkload.SpeechToText -> AiCapability.SPEECH_TO_TEXT
        is BenchmarkWorkload.TextToSpeech -> AiCapability.TEXT_TO_SPEECH
    }

private val BenchmarkWorkload.capabilityLabel: String
    get() = when (this) {
        is BenchmarkWorkload.Chat -> "Chat profile"
        is BenchmarkWorkload.SpeechToText -> "Speech-to-text profile"
        is BenchmarkWorkload.TextToSpeech -> "Text-to-speech profile"
    }

private val BenchmarkWorkload.workloadDescription: String
    get() = when (this) {
        is BenchmarkWorkload.Chat -> "Complete conversation context plus current draft · ${messages.size} messages"
        is BenchmarkWorkload.SpeechToText -> "${input.displayName} · ${input.durationMs} ms · ${languageCode}"
        is BenchmarkWorkload.TextToSpeech -> "${text.length} characters · $languageCode"
    }

private fun BenchmarkIterationResult.performanceText(capability: AiCapability): String = when (capability) {
    AiCapability.CHAT -> throughputPerSecond?.let { "${"%.2f".format(it)} tokens/s" } ?: "throughput unavailable"
    AiCapability.SPEECH_TO_TEXT,
    AiCapability.TEXT_TO_SPEECH,
    -> throughputPerSecond?.let { "${"%.2f".format(it)}× real time" } ?: "real-time factor unavailable"
    else -> throughputPerSecond?.let { "${"%.2f".format(it)} units/s" } ?: "throughput unavailable"
}

private fun InferenceResourceMetrics.batteryText(): String {
    if (!batteryMeasurementsAvailable) return "Battery: measurements unavailable"
    val readings = buildList {
        batteryEnergyDeltaNwh?.let { add(it.energyText()) }
        batteryChargeDeltaUah?.let { add(it.chargeText()) }
        averageBatteryCurrentUa?.let { current -> add("avg current: ${"%.0f".format(current / 1_000.0)} mA") }
    }
    return readings.takeIf(List<String>::isNotEmpty)
        ?.joinToString(separator = " · ", prefix = "Battery: ")
        ?: "Battery: available, but no numeric reading"
}

private fun Long.energyText(): String = if (this >= 0L) {
    "energy used: ${"%.3f".format(this / 1_000_000.0)} mWh"
} else {
    "energy change: ${"%.3f".format(this / 1_000_000.0)} mWh"
}

private fun Long.chargeText(): String = if (this >= 0L) {
    "charge used: ${"%.1f".format(this / 1_000.0)} mAh"
} else {
    "charge change: ${"%.1f".format(this / 1_000.0)} mAh"
}
