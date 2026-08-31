package com.dmitriim.localailab.feature.benchmark.impl.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.profiling.InferenceResourceMetrics
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkIterationResult
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkSessionSummary
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkWorkload
import com.dmitriim.localailab.feature.benchmark.impl.presentation.BenchmarkLabUiState

@Composable
internal fun BenchmarkResultsCard(
    state: BenchmarkLabUiState,
    workload: BenchmarkWorkload,
    onCopy: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BenchmarkResultsHeader(onCopy)
            BenchmarkMeasurementSummary(state)
            state.summary?.let { summary -> BenchmarkLatencyResults(summary, workload.capability) }
            state.completedIterations.last().telemetry.resources?.let { resources ->
                BenchmarkResourceResults(
                    resources = resources,
                    energy = state.summary?.totalBatteryEnergyDeltaNwh,
                )
            }
            BenchmarkIndividualResults(state.completedIterations)
        }
    }
}

@Composable
private fun BenchmarkResultsHeader(onCopy: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(CoreUiR.string.benchmark_benchmark_screen_33),
            style = MaterialTheme.typography.titleMedium,
        )
        TextButton(onClick = onCopy) {
            Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_34))
        }
    }
}

@Composable
private fun BenchmarkMeasurementSummary(state: BenchmarkLabUiState) {
    Column {
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
    }
}

@Composable
private fun BenchmarkLatencyResults(summary: BenchmarkSessionSummary, capability: AiCapability) {
    Column {
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
                ?.throughputText(capability)
                ?: stringResource(CoreUiR.string.benchmark_result_output_rate_unavailable),
            description = if (summary.medianThroughputPerSecond == null) {
                stringResource(CoreUiR.string.benchmark_result_output_rate_unavailable_description)
            } else {
                stringResource(CoreUiR.string.benchmark_result_output_rate_description)
            },
        )
    }
}

@Composable
private fun BenchmarkResourceResults(resources: InferenceResourceMetrics, energy: Long?) {
    Column {
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
        energy?.let {
            ResultMetric(
                label = stringResource(CoreUiR.string.ui_copy_50),
                value = it.energyText(),
                description = stringResource(CoreUiR.string.ui_description_26),
            )
        }
    }
}

@Composable
private fun BenchmarkIndividualResults(iterations: List<BenchmarkIterationResult>) {
    Column {
        HorizontalDivider()
        ResultSection(stringResource(CoreUiR.string.benchmark_result_individual_run_latency))
        Text(
            text = stringResource(CoreUiR.string.ui_copy_51),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        iterations.forEach { result ->
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

@Composable
private fun ResultSection(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun ResultMetric(label: String, value: String, description: String) {
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
