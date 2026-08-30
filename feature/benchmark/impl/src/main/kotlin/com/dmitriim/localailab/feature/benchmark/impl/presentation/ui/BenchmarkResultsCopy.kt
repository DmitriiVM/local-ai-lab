package com.dmitriim.localailab.feature.benchmark.impl.presentation.ui

import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.profiling.InferenceResourceMetrics
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkIterationResult
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkSessionSummary
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkStartupMode
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkWorkload
import com.dmitriim.localailab.feature.benchmark.impl.presentation.BenchmarkLabUiState

@Composable
internal fun BenchmarkLabUiState.resultsCopyText(workload: BenchmarkWorkload): String {
    val lines = benchmarkProfileCopyLines(workload).toMutableList()
    summary?.let { resultSummary ->
        lines += benchmarkLatencyCopyLines(resultSummary, workload)
    }
    completedIterations.lastOrNull()?.telemetry?.resources?.let { resources ->
        lines += benchmarkResourceCopyLines(resources, summary?.totalBatteryEnergyDeltaNwh)
    }
    lines += benchmarkIterationCopyLines(completedIterations)
    return lines.joinToString(separator = "\n")
}

@Composable
private fun BenchmarkLabUiState.benchmarkProfileCopyLines(workload: BenchmarkWorkload): List<String> {
    val traceStatus = if (completedIterations.any { it.telemetry.systemTraceEnabled }) {
        stringResource(CoreUiR.string.benchmark_copy_trace_recording)
    } else {
        stringResource(CoreUiR.string.benchmark_copy_trace_not_recording)
    }
    return listOf(
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
        stringResource(CoreUiR.string.benchmark_copy_external_trace, traceStatus),
    )
}

@Composable
private fun benchmarkLatencyCopyLines(
    summary: BenchmarkSessionSummary,
    workload: BenchmarkWorkload,
): List<String> = buildList {
    add("")
    add(stringResource(CoreUiR.string.benchmark_copy_latency_summary))
    add(
        stringResource(
            CoreUiR.string.benchmark_copy_typical_latency,
            summary.medianLatencyMs.durationText(),
        ),
    )
    add(
        stringResource(
            CoreUiR.string.benchmark_copy_p95_latency,
            summary.p95LatencyMs.durationText(),
        ),
    )
    add(
        stringResource(
            CoreUiR.string.benchmark_copy_latency_range,
            stringResource(
                CoreUiR.string.benchmark_result_latency_range,
                summary.minimumLatencyMs.durationText(),
                summary.maximumLatencyMs.durationText(),
            ),
        ),
    )
    val outputRate = summary.medianThroughputPerSecond
        ?.throughputText(workload.capability)
        ?: stringResource(CoreUiR.string.benchmark_result_output_rate_unavailable)
    add(stringResource(CoreUiR.string.benchmark_copy_output_rate, outputRate))
}

@Composable
private fun benchmarkResourceCopyLines(
    resources: InferenceResourceMetrics,
    energy: Long?,
): List<String> = buildList {
    add("")
    add(stringResource(CoreUiR.string.benchmark_copy_resources_last_run))
    add(
        stringResource(
            CoreUiR.string.benchmark_copy_cpu_usage,
            resources.averageProcessCpuPercent.percentText(),
            resources.peakProcessCpuPercent.percentText(),
        ),
    )
    add(
        stringResource(
            CoreUiR.string.benchmark_copy_peak_memory,
            resources.peakPssBytes.memoryText(),
        ),
    )
    add(
        stringResource(
            CoreUiR.string.benchmark_copy_thermal_state,
            resources.thermalStatusEnd.thermalStatusText(),
        ),
    )
    add(
        stringResource(
            CoreUiR.string.benchmark_copy_battery_use,
            resources.batteryValueText(),
        ),
    )
    energy?.let { value ->
        add(stringResource(CoreUiR.string.benchmark_copy_session_energy, value.energyText()))
    }
}

@Composable
private fun benchmarkIterationCopyLines(iterations: List<BenchmarkIterationResult>): List<String> = buildList {
    add("")
    add(stringResource(CoreUiR.string.benchmark_copy_individual_run_latency))
    for (result in iterations) {
        add(
            stringResource(
                CoreUiR.string.benchmark_copy_run,
                result.iteration,
                result.latencyMs.durationText(),
            ),
        )
    }
}

internal val BenchmarkWorkload.capability: AiCapability
    get() = when (this) {
        is BenchmarkWorkload.Chat -> AiCapability.CHAT
        is BenchmarkWorkload.SpeechToText -> AiCapability.SPEECH_TO_TEXT
        is BenchmarkWorkload.TextToSpeech -> AiCapability.TEXT_TO_SPEECH
    }

@Composable
internal fun BenchmarkWorkload.capabilityLabel(): String = stringResource(
    when (this) {
        is BenchmarkWorkload.Chat -> CoreUiR.string.benchmark_chat_profile
        is BenchmarkWorkload.SpeechToText -> CoreUiR.string.benchmark_stt_profile
        is BenchmarkWorkload.TextToSpeech -> CoreUiR.string.benchmark_tts_profile
    },
)

@Composable
internal fun BenchmarkWorkload.workloadDescription(): String = when (this) {
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
internal fun BenchmarkStartupMode.displayName(): String = stringResource(
    when (this) {
        BenchmarkStartupMode.WARM -> CoreUiR.string.benchmark_startup_mode_warm
        BenchmarkStartupMode.COLD -> CoreUiR.string.benchmark_startup_mode_cold
    },
)

@Composable
internal fun Double?.percentText(): String = if (this == null) {
    stringResource(CoreUiR.string.benchmark_value_unavailable)
} else {
    stringResource(CoreUiR.string.benchmark_value_percent, this)
}

@Composable
internal fun Long?.durationText(): String = if (this == null) {
    stringResource(CoreUiR.string.benchmark_value_unavailable)
} else if (this >= 1_000L) {
    stringResource(CoreUiR.string.benchmark_value_duration_seconds, this / 1_000.0)
} else {
    stringResource(CoreUiR.string.benchmark_value_duration_millis, this)
}

@Composable
internal fun Double.throughputText(capability: AiCapability): String = stringResource(
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
internal fun Long?.memoryText(): String = if (this == null) {
    stringResource(CoreUiR.string.benchmark_value_unavailable)
} else {
    stringResource(CoreUiR.string.benchmark_value_memory_mib, this / 1_048_576.0)
}

@Composable
internal fun Int?.thermalStatusText(): String = stringResource(
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
internal fun InferenceResourceMetrics.batteryValueText(): String {
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
internal fun Long.energyText(): String = if (this >= 0L) {
    stringResource(CoreUiR.string.benchmark_energy_used, this / 1_000_000.0)
} else {
    stringResource(CoreUiR.string.benchmark_energy_change, this / 1_000_000.0)
}

@Composable
internal fun Long.chargeText(): String = if (this >= 0L) {
    stringResource(CoreUiR.string.benchmark_charge_used, this / 1_000.0)
} else {
    stringResource(CoreUiR.string.benchmark_charge_change, this / 1_000.0)
}
