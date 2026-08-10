package com.dmitriim.localaiplayground.feature.benchmark.presentation.ui

import android.os.PowerManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.performance.BenchmarkWorkload
import com.dmitriim.localaiplayground.core.performance.InferenceResourceMetrics
import com.dmitriim.localaiplayground.core.ui.layout.LocalAppDimensions
import com.dmitriim.localaiplayground.feature.benchmark.presentation.BenchmarkLabUiState

@Composable
fun BenchmarkScreen(
    state: BenchmarkLabUiState,
    onWarmupsChange: (Int) -> Unit,
    onMeasuredChange: (Int) -> Unit,
    onToggleStartupMode: () -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    val clipboard = LocalClipboardManager.current
    val workload = state.workload
    val systemNavigationPadding = if (dimensions.bottomNavigationOverlayClearance == 0.dp) {
        Modifier.navigationBarsPadding()
    } else {
        Modifier
    }
    Column(
        modifier = Modifier.fillMaxSize().then(systemNavigationPadding).verticalScroll(rememberScrollState()).padding(
            start = dimensions.screenPadding,
            top = dimensions.topBarOverlayClearance + 20.dp,
            end = dimensions.screenPadding,
            bottom = 44.dp + dimensions.bottomNavigationOverlayClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacing),
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Repeat the selected workload to measure on-device speed, CPU, memory, thermal state, and supported battery use. Warm-ups are excluded from the saved results.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
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
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Profile configuration", style = MaterialTheme.typography.titleMedium)
                IterationControl(
                    label = "Warm-ups",
                    description = "Run first and exclude from results",
                    value = state.warmupIterations,
                    onDecrement = { onWarmupsChange(state.warmupIterations - 1) },
                    onIncrement = { onWarmupsChange(state.warmupIterations + 1) },
                    enabled = !state.isRunning,
                )
                IterationControl(
                    label = "Measured runs",
                    description = "Save each run to Runs history",
                    value = state.measuredIterations,
                    onDecrement = { onMeasuredChange(state.measuredIterations - 1) },
                    onIncrement = { onMeasuredChange(state.measuredIterations + 1) },
                    enabled = !state.isRunning,
                )
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Startup mode", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (state.startupMode.name == "WARM") {
                                "Reuse the runtime between iterations"
                            } else {
                                "Reload the runtime for every iteration"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    AssistChip(
                        onClick = onToggleStartupMode,
                        enabled = !state.isRunning,
                        label = { Text(state.startupMode.name.lowercase().replaceFirstChar(Char::titlecase)) },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStart, enabled = !state.isRunning) { Text("Start profiling") }
            if (state.isRunning) Button(onClick = onCancel) { Text("Cancel") }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
        if (state.completedIterations.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Measured results", style = MaterialTheme.typography.titleMedium)
                        TextButton(
                            onClick = { clipboard.setText(AnnotatedString(state.resultsCopyText(workload))) },
                        ) {
                            Text("Copy results")
                        }
                    }
                    Text(
                        text = "${state.completedIterations.size} saved measurement${if (state.completedIterations.size == 1) "" else "s"}. Latency is the time from inference start to completion; lower is faster.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = if (state.completedIterations.any { it.telemetry.traceActive }) {
                            "External trace is recording. Use it to correlate these runs with system GPU and scheduling data."
                        } else {
                            "External trace is not recording. In-app CPU, memory, thermal, and supported battery telemetry is still collected."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    state.summary?.let { summary ->
                        ResultSection("Latency summary")
                        ResultMetric(
                            label = "Typical latency",
                            value = summary.medianLatencyMs.durationText(),
                            description = "Median completion time across the measured runs.",
                        )
                        ResultMetric(
                            label = "p95 latency",
                            value = summary.p95LatencyMs.durationText(),
                            description = "95% of runs completed within this time; useful for spotting slow runs.",
                        )
                        ResultMetric(
                            label = "Latency range",
                            value = "${summary.minimumLatencyMs.durationText()}–${summary.maximumLatencyMs.durationText()}",
                            description = "Fastest to slowest measured completion time.",
                        )
                        ResultMetric(
                            label = "Output rate",
                            value = summary.medianThroughputPerSecond?.throughputText(workload.capability) ?: "Unavailable",
                            description = if (summary.medianThroughputPerSecond == null) {
                                "This runtime did not report output units, so only latency can be compared."
                            } else {
                                "Median generated output per second across measured runs. Higher is faster."
                            },
                        )
                    }
                    state.completedIterations.last().telemetry.resources?.let { resources ->
                        ResultSection("Resources — last measured run")
                        ResultMetric(
                            label = "CPU usage",
                            value = "${resources.averageProcessCpuPercent.percentText()} avg · ${resources.peakProcessCpuPercent.percentText()} peak",
                            description = "Process CPU time over wall time. 100% equals one fully used CPU core, so multi-core work can exceed 100%.",
                        )
                        ResultMetric(
                            label = "Peak app memory",
                            value = resources.peakPssBytes.memoryText(),
                            description = "Largest proportional set size (PSS) during this run. PSS estimates shared memory proportionally.",
                        )
                        ResultMetric(
                            label = "Thermal state",
                            value = resources.thermalStatusEnd.thermalStatusText(),
                            description = "Android device thermal status at the end of the run. Severe or higher stops a profile to protect the device.",
                        )
                        ResultMetric(
                            label = "Battery use",
                            value = resources.batteryValueText(),
                            description = "Battery reporting is coarse for short profiles. Longer workloads produce more useful readings; current sign follows the device power driver.",
                        )
                        state.summary?.totalBatteryEnergyDeltaNwh?.let { energy ->
                            ResultMetric(
                                label = "Session energy",
                                value = energy.energyText(),
                                description = "Energy-counter change over all measured runs. Availability depends on the device.",
                            )
                        }
                    }
                    HorizontalDivider()
                    ResultSection("Individual run latency")
                    Text(
                        text = "Completion time for each saved measurement. Lower is faster.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    state.completedIterations.forEach { result ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Run ${result.iteration}")
                            Text(result.latencyMs.durationText(), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultSection(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun ResultMetric(
    label: String,
    value: String,
    description: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun BenchmarkLabUiState.resultsCopyText(workload: BenchmarkWorkload): String = buildList {
    add("Profile results")
    add(workload.capabilityLabel)
    add("Model: ${workload.modelDisplayName}")
    add("Workload: ${workload.workloadDescription}")
    add(
        "Configuration: $warmupIterations warm-up${if (warmupIterations == 1) "" else "s"}, " +
            "$measuredIterations measured run${if (measuredIterations == 1) "" else "s"}, " +
            "${startupMode.name.lowercase().replaceFirstChar(Char::titlecase)} startup",
    )
    add(
        "External trace: " + if (completedIterations.any { it.telemetry.traceActive }) {
            "recording"
        } else {
            "not recording (in-app telemetry collected)"
        },
    )
    summary?.let { resultSummary ->
        add("")
        add("Latency summary (lower is faster)")
        add("Typical latency (median): ${resultSummary.medianLatencyMs.durationText()}")
        add("p95 latency: ${resultSummary.p95LatencyMs.durationText()}")
        add("Latency range: ${resultSummary.minimumLatencyMs.durationText()}–${resultSummary.maximumLatencyMs.durationText()}")
        add("Output rate: ${resultSummary.medianThroughputPerSecond?.throughputText(workload.capability) ?: "Unavailable"}")
    }
    completedIterations.lastOrNull()?.telemetry?.resources?.let { resources ->
        add("")
        add("Resources (last measured run)")
        add("CPU usage: ${resources.averageProcessCpuPercent.percentText()} average · ${resources.peakProcessCpuPercent.percentText()} peak")
        add("Peak app memory: ${resources.peakPssBytes.memoryText()} PSS")
        add("Thermal state: ${resources.thermalStatusEnd.thermalStatusText()}")
        add("Battery use: ${resources.batteryValueText()}")
        summary?.totalBatteryEnergyDeltaNwh?.let { add("Session energy: ${it.energyText()}") }
    }
    add("")
    add("Individual run latency (lower is faster)")
    completedIterations.forEach { result -> add("Run ${result.iteration}: ${result.latencyMs.durationText()}") }
}.joinToString(separator = "\n")

@Composable
private fun IterationControl(
    label: String,
    description: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDecrement, enabled = enabled) { Text("−") }
            Text(
                text = value.toString(),
                modifier = Modifier.width(28.dp),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onIncrement, enabled = enabled) { Text("+") }
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
        is BenchmarkWorkload.SpeechToText -> "${input.displayName} · ${input.durationMs} ms · $languageCode"
        is BenchmarkWorkload.TextToSpeech -> "${text.length} characters · $languageCode"
    }

private fun Double?.percentText(): String = this?.let { "%.1f%%".format(it) } ?: "Unavailable"

private fun Long?.durationText(): String = this?.let { durationMs ->
    if (durationMs >= 1_000L) "%.2f s".format(durationMs / 1_000.0) else "$durationMs ms"
} ?: "Unavailable"

private fun Double.throughputText(capability: AiCapability): String = when (capability) {
    AiCapability.CHAT -> "${"%.2f".format(this)} tokens/s"
    AiCapability.SPEECH_TO_TEXT,
    AiCapability.TEXT_TO_SPEECH,
    -> "${"%.2f".format(this)}× real time"
    else -> "${"%.2f".format(this)} units/s"
}

private fun Long?.memoryText(): String = this?.let { "%.1f MiB".format(it / 1_048_576.0) } ?: "Unavailable"

private fun Int?.thermalStatusText(): String = when (this) {
    PowerManager.THERMAL_STATUS_NONE -> "None"
    PowerManager.THERMAL_STATUS_LIGHT -> "Light"
    PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
    PowerManager.THERMAL_STATUS_SEVERE -> "Severe"
    PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
    PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
    PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown"
    null -> "Unavailable"
    else -> "Unknown"
}

private fun InferenceResourceMetrics.batteryValueText(): String {
    if (!batteryMeasurementsAvailable) return "Unavailable"
    val readings = buildList {
        batteryEnergyDeltaNwh?.takeIf { kotlin.math.abs(it) >= 10_000L }?.let { add(it.energyText()) }
        batteryChargeDeltaUah?.takeIf { kotlin.math.abs(it) >= 10L }?.let { add(it.chargeText()) }
        averageBatteryCurrentUa
            ?.takeIf { kotlin.math.abs(it) >= 500.0 }
            ?.let { current -> add("${"%.1f".format(current / 1_000.0)} mA average") }
    }
    return readings.takeIf(List<String>::isNotEmpty)
        ?.joinToString(separator = " · ")
        ?: "No measurable change"
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
