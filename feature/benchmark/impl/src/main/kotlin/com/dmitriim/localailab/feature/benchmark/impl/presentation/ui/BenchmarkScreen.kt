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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.profiling.InferenceResourceMetrics
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.layout.LocalAppDimensions
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkWorkload
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkStartupMode
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
        Text(
            text = stringResource(CoreUiR.string.benchmark_benchmark_screen_26),
            style = MaterialTheme.typography.headlineMedium,
        )
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
                Text(
                    text = stringResource(CoreUiR.string.benchmark_benchmark_screen_28),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(CoreUiR.string.benchmark_benchmark_screen_29),
                    style = MaterialTheme.typography.titleMedium,
                )
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
                        Text(
                            text = stringResource(CoreUiR.string.benchmark_benchmark_screen_30),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = if (state.startupMode == BenchmarkStartupMode.WARM) {
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
                        label = { Text(state.startupMode.displayName()) },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStart, enabled = !state.isRunning) {
                Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_31))
            }
            if (state.isRunning) {
                Button(onClick = onCancel) {
                    Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_32))
                }
            }
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
                        Text(
                            text = stringResource(CoreUiR.string.benchmark_benchmark_screen_33),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        TextButton(
                            onClick = { clipboard.setText(AnnotatedString(resultsCopyText)) },
                        ) {
                            Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_34))
                        }
                    }
                    Text(
                        text = stringResource(
                            CoreUiR.string.benchmark_measurement_summary,
                            pluralStringResource(
                                CoreUiR.plurals.benchmark_measurement_count,
                                state.completedIterations.size,
                                state.completedIterations.size,
                            ),
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
                        ResultSection(stringResource(CoreUiR.string.benchmark_result_latency_summary))
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
                            value = stringResource(
                                CoreUiR.string.benchmark_result_latency_range,
                                summary.minimumLatencyMs.durationText(),
                                summary.maximumLatencyMs.durationText(),
                            ),
                            description = stringResource(CoreUiR.string.ui_description_21),
                        )
                        ResultMetric(
                            label = stringResource(CoreUiR.string.ui_copy_45),
                            value = summary.medianThroughputPerSecond
                                ?.throughputText(workload.capability)
                                ?: stringResource(CoreUiR.string.benchmark_result_output_rate_unavailable),
                            description = if (summary.medianThroughputPerSecond == null) {
                                stringResource(CoreUiR.string.benchmark_result_output_rate_unavailable_description)
                            } else {
                                stringResource(CoreUiR.string.benchmark_result_output_rate_description)
                            },
                        )
                    }
                    state.completedIterations.last().telemetry.resources?.let { resources ->
                        ResultSection(stringResource(CoreUiR.string.benchmark_result_resources_last_run))
                        ResultMetric(
                            label = stringResource(CoreUiR.string.ui_copy_46),
                            value = stringResource(
                                CoreUiR.string.benchmark_result_cpu_usage,
                                resources.averageProcessCpuPercent.percentText(),
                                resources.peakProcessCpuPercent.percentText(),
                            ),
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
                    ResultSection(stringResource(CoreUiR.string.benchmark_result_individual_run_latency))
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
private fun BenchmarkLabUiState.resultsCopyText(workload: BenchmarkWorkload): String {
    val lines = mutableListOf(
        stringResource(CoreUiR.string.benchmark_copy_profile_results),
        workload.capabilityLabel(),
        stringResource(CoreUiR.string.benchmark_copy_model, workload.modelDisplayName),
        stringResource(CoreUiR.string.benchmark_copy_workload, workload.workloadDescription()),
        stringResource(
            CoreUiR.string.benchmark_copy_configuration,
            pluralStringResource(
                CoreUiR.plurals.benchmark_warmup_count,
                warmupIterations,
                warmupIterations,
            ),
            pluralStringResource(
                CoreUiR.plurals.benchmark_measured_run_count,
                measuredIterations,
                measuredIterations,
            ),
            startupMode.displayName(),
        ),
    )
    val traceStatus = if (completedIterations.any { it.telemetry.systemTraceEnabled }) {
        stringResource(CoreUiR.string.benchmark_copy_trace_recording)
    } else {
        stringResource(CoreUiR.string.benchmark_copy_trace_not_recording)
    }
    lines += stringResource(CoreUiR.string.benchmark_copy_external_trace, traceStatus)

    val resultSummary = summary
    if (resultSummary != null) {
        lines += ""
        lines += stringResource(CoreUiR.string.benchmark_copy_latency_summary)
        lines += stringResource(
            CoreUiR.string.benchmark_copy_typical_latency,
            resultSummary.medianLatencyMs.durationText(),
        )
        lines += stringResource(
            CoreUiR.string.benchmark_copy_p95_latency,
            resultSummary.p95LatencyMs.durationText(),
        )
        lines += stringResource(
            CoreUiR.string.benchmark_copy_latency_range,
            stringResource(
                CoreUiR.string.benchmark_result_latency_range,
                resultSummary.minimumLatencyMs.durationText(),
                resultSummary.maximumLatencyMs.durationText(),
            ),
        )
        val outputRate = resultSummary.medianThroughputPerSecond
            ?.throughputText(workload.capability)
            ?: stringResource(CoreUiR.string.benchmark_result_output_rate_unavailable)
        lines += stringResource(CoreUiR.string.benchmark_copy_output_rate, outputRate)
    }

    val resources = completedIterations.lastOrNull()?.telemetry?.resources
    if (resources != null) {
        lines += ""
        lines += stringResource(CoreUiR.string.benchmark_copy_resources_last_run)
        lines += stringResource(
            CoreUiR.string.benchmark_copy_cpu_usage,
            resources.averageProcessCpuPercent.percentText(),
            resources.peakProcessCpuPercent.percentText(),
        )
        lines += stringResource(
            CoreUiR.string.benchmark_copy_peak_memory,
            resources.peakPssBytes.memoryText(),
        )
        lines += stringResource(
            CoreUiR.string.benchmark_copy_thermal_state,
            resources.thermalStatusEnd.thermalStatusText(),
        )
        lines += stringResource(
            CoreUiR.string.benchmark_copy_battery_use,
            resources.batteryValueText(),
        )
        val energy = summary?.totalBatteryEnergyDeltaNwh
        if (energy != null) {
            lines += stringResource(CoreUiR.string.benchmark_copy_session_energy, energy.energyText())
        }
    }

    lines += ""
    lines += stringResource(CoreUiR.string.benchmark_copy_individual_run_latency)
    for (result in completedIterations) {
        lines += stringResource(
            CoreUiR.string.benchmark_copy_run,
            result.iteration,
            result.latencyMs.durationText(),
        )
    }
    return lines.joinToString(separator = "\n")
}

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
            TextButton(onClick = onDecrement, enabled = enabled) {
                Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_35))
            }
            Text(
                text = value.toString(),
                modifier = Modifier.width(28.dp),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onIncrement, enabled = enabled) {
                Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_36))
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
    is BenchmarkWorkload.SpeechToText -> stringResource(
        CoreUiR.string.benchmark_workload_stt,
        input.displayName,
        input.durationMs,
        languageCode,
    )
    is BenchmarkWorkload.TextToSpeech -> stringResource(
        CoreUiR.string.benchmark_workload_tts,
        text.length,
        languageCode,
    )
}

@Composable
private fun BenchmarkStartupMode.displayName(): String = stringResource(
    when (this) {
        BenchmarkStartupMode.WARM -> CoreUiR.string.benchmark_startup_mode_warm
        BenchmarkStartupMode.COLD -> CoreUiR.string.benchmark_startup_mode_cold
    },
)

@Composable
private fun Double?.percentText(): String = if (this == null) {
    stringResource(CoreUiR.string.benchmark_value_unavailable)
} else {
    stringResource(CoreUiR.string.benchmark_value_percent, this)
}

@Composable
private fun Long?.durationText(): String = if (this == null) {
    stringResource(CoreUiR.string.benchmark_value_unavailable)
} else if (this >= 1_000L) {
    stringResource(CoreUiR.string.benchmark_value_duration_seconds, this / 1_000.0)
} else {
    stringResource(CoreUiR.string.benchmark_value_duration_millis, this)
}

@Composable
private fun Double.throughputText(capability: AiCapability): String = stringResource(
    when (capability) {
        AiCapability.CHAT -> CoreUiR.string.benchmark_value_chat_rate
        AiCapability.SPEECH_TO_TEXT,
        AiCapability.TEXT_TO_SPEECH,
        -> CoreUiR.string.benchmark_value_realtime_rate
        else -> CoreUiR.string.benchmark_value_units_rate
    },
    this,
)

@Composable
private fun Long?.memoryText(): String = if (this == null) {
    stringResource(CoreUiR.string.benchmark_value_unavailable)
} else {
    stringResource(CoreUiR.string.benchmark_value_memory_mib, this / 1_048_576.0)
}

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

@Composable
private fun InferenceResourceMetrics.batteryValueText(): String {
    if (!batteryMeasurementsAvailable) return stringResource(CoreUiR.string.benchmark_value_unavailable)
    val readings = mutableListOf<String>()
    val energy = batteryEnergyDeltaNwh
    if (energy != null && kotlin.math.abs(energy) >= 10_000L) {
        readings += energy.energyText()
    }
    val charge = batteryChargeDeltaUah
    if (charge != null && kotlin.math.abs(charge) >= 10L) {
        readings += charge.chargeText()
    }
    val current = averageBatteryCurrentUa
    if (current != null && kotlin.math.abs(current) >= 500.0) {
        readings += stringResource(CoreUiR.string.benchmark_battery_current, current / 1_000.0)
    }
    return readings.joinToString(separator = " · ")
        .ifEmpty { stringResource(CoreUiR.string.benchmark_battery_no_measurable_change) }
}

@Composable
private fun Long.energyText(): String = if (this >= 0L) {
    stringResource(CoreUiR.string.benchmark_energy_used, this / 1_000_000.0)
} else {
    stringResource(CoreUiR.string.benchmark_energy_change, this / 1_000_000.0)
}

@Composable
private fun Long.chargeText(): String = if (this >= 0L) {
    stringResource(CoreUiR.string.benchmark_charge_used, this / 1_000.0)
} else {
    stringResource(CoreUiR.string.benchmark_charge_change, this / 1_000.0)
}
