package com.dmitriim.localailab.feature.benchmark.impl.presentation.ui

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
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.layout.LocalAppDimensions
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkStartupMode
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkWorkload
import com.dmitriim.localailab.feature.benchmark.impl.presentation.BenchmarkLabUiState

@Composable
fun BenchmarkScreen(
    state: BenchmarkLabUiState,
    onWarmupsChange: (Int) -> Unit,
    onMeasuredChange: (Int) -> Unit,
    onToggleStartupMode: () -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = modifier.fillMaxSize().then(systemNavigationPadding).verticalScroll(rememberScrollState()).padding(
            start = dimensions.screenPadding,
            top = dimensions.topBarOverlayClearance + 20.dp,
            end = dimensions.screenPadding,
            bottom = 44.dp + dimensions.bottomNavigationOverlayClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacing),
    ) {
        BenchmarkHeader()
        if (workload == null) {
            Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_27))
            return@Column
        }
        BenchmarkProfileCard(workload)
        BenchmarkConfigurationCard(
            state = state,
            onWarmupsChange = onWarmupsChange,
            onMeasuredChange = onMeasuredChange,
            onToggleStartupMode = onToggleStartupMode,
        )
        BenchmarkRunActions(
            isRunning = state.isRunning,
            onStart = onStart,
            onCancel = onCancel,
        )
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
        if (state.completedIterations.isNotEmpty()) {
            val copyText = state.resultsCopyText(workload)
            BenchmarkResultsCard(
                state = state,
                workload = workload,
                onCopy = { clipboard.setText(AnnotatedString(copyText)) },
            )
        }
    }
}

@Composable
private fun BenchmarkHeader() {
    Column {
        Text(
            text = stringResource(CoreUiR.string.benchmark_benchmark_screen_26),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(CoreUiR.string.ui_copy_38),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun BenchmarkProfileCard(workload: BenchmarkWorkload) {
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
}

@Composable
private fun BenchmarkConfigurationCard(
    state: BenchmarkLabUiState,
    onWarmupsChange: (Int) -> Unit,
    onMeasuredChange: (Int) -> Unit,
    onToggleStartupMode: () -> Unit,
) {
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
            StartupModeControl(
                mode = state.startupMode,
                enabled = !state.isRunning,
                onToggle = onToggleStartupMode,
            )
        }
    }
}

@Composable
private fun StartupModeControl(
    mode: BenchmarkStartupMode,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
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
                text = if (mode == BenchmarkStartupMode.WARM) {
                    stringResource(CoreUiR.string.benchmark_reuse_runtime)
                } else {
                    stringResource(CoreUiR.string.benchmark_reload_runtime)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        AssistChip(
            onClick = onToggle,
            enabled = enabled,
            label = { Text(mode.displayName()) },
        )
    }
}

@Composable
private fun BenchmarkRunActions(
    isRunning: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onStart, enabled = !isRunning) {
            Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_31))
        }
        if (isRunning) {
            Button(onClick = onCancel) {
                Text(stringResource(CoreUiR.string.benchmark_benchmark_screen_32))
            }
        }
    }
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
