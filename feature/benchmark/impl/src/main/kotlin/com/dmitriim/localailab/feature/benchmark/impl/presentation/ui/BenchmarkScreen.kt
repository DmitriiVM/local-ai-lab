package com.dmitriim.localailab.feature.benchmark.impl.presentation.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkWorkload
import com.dmitriim.localailab.ai.api.profiling.InferenceResourceMetrics
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.layout.LocalAppDimensions
import com.dmitriim.localailab.feature.benchmark.impl.presentation.BenchmarkLabUiState

@Composable
@Suppress("LongMethod") // This declarative screen is organized by its visual sections.
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
        Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_26), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = stringResource(CoreUiR.string.ui_copy_38),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (workload == null) {
            Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_27))
            return@Column
        }
        val resultsCopyText = state.resultsCopyText(workload)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(workload.capabilityLabel(), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_format_2, workload.modelDisplayName))
                Text(workload.workloadDescription(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_28), style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_29), style = MaterialTheme.typography.titleMedium)
                IterationControl(
                    label = stringResource(CoreUiR.string.ui_copy_39),
                    description = stringResource(CoreUiR.string.ui_description_17),
                    value = state.warmupIterations,
                    onDecrement = { onWarmupsChange(state.warmupIterations - 1) },
                    onIncrement = { onWarmupsChange(state.warmupIterations + 1) },
                    enabled = !state.isRunning,
                )
                IterationControl(
                    label = stringResource(CoreUiR.string.ui_copy_40),
                    description = stringResource(CoreUiR.string.ui_description_18),
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
                        Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_30), style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (state.startupMode.name == "WARM") {
                                stringResource(CoreUiR.string.benchmark_reuse_runtime)
                            } else {
                                stringResource(CoreUiR.string.benchmark_reload_runtime)
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
            Button(onClick = onStart, enabled = !state.isRunning) { Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_31)) }
            if (state.isRunning) Button(onClick = onCancel) { Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_32)) }
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
                        Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_33), style = MaterialTheme.typography.titleMedium)
                        TextButton(
                            onClick = { clipboard.setText(AnnotatedString(resultsCopyText)) },
                        ) {
                            Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_34))
                        }
                    }
                    Text(
                        text = stringResource(
                            CoreUiR.string.benchmark_measurement_summary,
                            state.completedIterations.size,
                            if (state.completedIterations.size == 1) "" else "s",
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = if (state.completedIterations.any { it.telemetry.systemTraceEnabled }) {
                            stringResource(CoreUiR.string.benchmark_trace_recording)
                        } else {
                            stringResource(CoreUiR.string.benchmark_trace_not_recording)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    state.summary?.let { summary ->
                        ResultSection("Latency summary")
                        ResultMetric(
                            label = stringResource(CoreUiR.string.ui_copy_42),
                            value = summary.medianLatencyMs.durationText(),
                            description = stringResource(CoreUiR.string.ui_description_19),
                        )
                        ResultMetric(
                            label = stringResource(CoreUiR.string.ui_copy_43),
                            value = summary.p95LatencyMs.durationText(),
                            description = stringResource(CoreUiR.string.ui_description_20),
                        )
                        ResultMetric(
                            label = stringResource(CoreUiR.string.ui_copy_44),
                            value = "${summary.minimumLatencyMs.durationText()}–${summary.maximumLatencyMs.durationText()}",
                            description = stringResource(CoreUiR.string.ui_description_21),
                        )
                        ResultMetric(
                            label = stringResource(CoreUiR.string.ui_copy_45),
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
                            label = stringResource(CoreUiR.string.ui_copy_46),
                            value = "${resources.averageProcessCpuPercent.percentText()} avg · ${resources.peakProcessCpuPercent.percentText()} peak",
                            description = stringResource(CoreUiR.string.ui_description_22),
                        )
                        ResultMetric(
                            label = stringResource(CoreUiR.string.ui_copy_47),
                            value = resources.peakPssBytes.memoryText(),
                            description = stringResource(CoreUiR.string.ui_description_23),
                        )
                        ResultMetric(
                            label = stringResource(CoreUiR.string.ui_copy_48),
                            value = resources.thermalStatusEnd.thermalStatusText(),
                            description = stringResource(CoreUiR.string.ui_description_24),
                        )
                        ResultMetric(
                            label = stringResource(CoreUiR.string.ui_copy_49),
                            value = resources.batteryValueText(),
                            description = stringResource(CoreUiR.string.ui_description_25),
                        )
                        state.summary?.totalBatteryEnergyDeltaNwh?.let { energy ->
                            ResultMetric(
                                label = stringResource(CoreUiR.string.ui_copy_50),
                                value = energy.energyText(),
                                description = stringResource(CoreUiR.string.ui_description_26),
                            )
                        }
                    }
                    HorizontalDivider()
                    ResultSection("Individual run latency")
                    Text(
                        text = stringResource(CoreUiR.string.ui_copy_51),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    state.completedIterations.forEach { result ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_format_3, result.iteration))
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

@Composable
private fun BenchmarkLabUiState.resultsCopyText(workload: BenchmarkWorkload): String = buildList {
    add("Profile results")
    add(workload.capabilityLabel())
    add("Model: ${workload.modelDisplayName}")
    add("Workload: ${workload.workloadDescription()}")
    add(
        "Configuration: $warmupIterations warm-up${if (warmupIterations == 1) "" else "s"}, " +
            "$measuredIterations measured run${if (measuredIterations == 1) "" else "s"}, " +
            "${startupMode.name.lowercase().replaceFirstChar(Char::titlecase)} startup",
    )
    add(
        "External trace: " + if (completedIterations.any { it.telemetry.systemTraceEnabled }) {
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
            TextButton(onClick = onDecrement, enabled = enabled) { Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_35)) }
            Text(
                text = value.toString(),
                modifier = Modifier.width(28.dp),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onIncrement, enabled = enabled) { Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_36)) }
        }
    }
}

private val BenchmarkWorkload.capability: AiCapability
    get() = when (this) {
        is BenchmarkWorkload.Chat -> AiCapability.CHAT
        is BenchmarkWorkload.SpeechToText -> AiCapability.SPEECH_TO_TEXT
        is BenchmarkWorkload.TextToSpeech -> AiCapability.TEXT_TO_SPEECH
    }

@Composable
private fun BenchmarkWorkload.capabilityLabel(): String = stringResource(
    when (this) {
        is BenchmarkWorkload.Chat -> CoreUiR.string.benchmark_chat_profile
        is BenchmarkWorkload.SpeechToText -> CoreUiR.string.benchmark_stt_profile
        is BenchmarkWorkload.TextToSpeech -> CoreUiR.string.benchmark_tts_profile
    },
)

@Composable
private fun BenchmarkWorkload.workloadDescription(): String = when (this) {
    is BenchmarkWorkload.Chat -> stringResource(CoreUiR.string.benchmark_workload_chat, messages.size)
    is BenchmarkWorkload.SpeechToText -> stringResource(CoreUiR.string.benchmark_workload_stt, input.displayName, input.durationMs, languageCode)
    is BenchmarkWorkload.TextToSpeech -> stringResource(CoreUiR.string.benchmark_workload_tts, text.length, languageCode)
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

@Composable
private fun Int?.thermalStatusText(): String = stringResource(
    when (this) {
        PowerManager.THERMAL_STATUS_NONE -> CoreUiR.string.thermal_none
        PowerManager.THERMAL_STATUS_LIGHT -> CoreUiR.string.thermal_light
        PowerManager.THERMAL_STATUS_MODERATE -> CoreUiR.string.thermal_moderate
        PowerManager.THERMAL_STATUS_SEVERE -> CoreUiR.string.thermal_severe
        PowerManager.THERMAL_STATUS_CRITICAL -> CoreUiR.string.thermal_critical
        PowerManager.THERMAL_STATUS_EMERGENCY -> CoreUiR.string.thermal_emergency
        PowerManager.THERMAL_STATUS_SHUTDOWN -> CoreUiR.string.thermal_shutdown
        null -> CoreUiR.string.ui_unavailable
        else -> CoreUiR.string.ui_unknown
    },
)

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
