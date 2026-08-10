package com.dmitriim.localaiplayground.feature.device.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.engine.EngineAvailability
import com.dmitriim.localaiplayground.core.ui.R as CoreUiR
import com.dmitriim.localaiplayground.core.ui.layout.LocalAppDimensions
import com.dmitriim.localaiplayground.feature.device.presentation.DeviceUiState

@Composable
fun DeviceScreen(
    state: DeviceUiState,
    onRefresh: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    val systemNavigationPadding = if (dimensions.bottomNavigationOverlayClearance == 0.dp) {
        Modifier.navigationBarsPadding()
    } else {
        Modifier
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(systemNavigationPadding)
            .padding(horizontal = dimensions.screenPadding),
        contentPadding = PaddingValues(
            top = dimensions.topBarOverlayClearance,
            bottom = 24.dp + dimensions.bottomNavigationOverlayClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacing),
    ) {
        item {
            Text(
                text = stringResource(CoreUiR.string.ui_copy_52),
                modifier = Modifier.padding(top = 20.dp),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        state.snapshot?.let { snapshot ->
            item {
                DeviceInfoCard(
                    title = snapshot.deviceName,
                    lines = listOf(
                        snapshot.androidVersion,
                        "ABIs: ${snapshot.abis}",
                        "Memory: ${snapshot.availableMemory} available / ${snapshot.totalMemory} total",
                        "App storage available: ${snapshot.availableStorage}",
                        snapshot.cpuInfo,
                        snapshot.batteryState,
                        snapshot.thermalState,
                    ),
                )
            }
        }
        state.interruptionMessage?.let { message ->
            item {
                DeviceInfoCard(
                    title = stringResource(CoreUiR.string.ui_copy_53),
                    lines = listOf(message),
                    isError = true,
                )
            }
        }
        items(
            count = state.engines.size,
            key = { index -> state.engines[index].descriptor.id.value },
        ) { index ->
            val engine = state.engines[index]
            DeviceInfoCard(
                title = engine.descriptor.displayName,
                lines = listOf(
                    when (engine) {
                        is EngineAvailability.Available ->
                            "Available • ${engine.effectiveComputePreference} • ${engine.detail}"
                        is EngineAvailability.Unsupported -> stringResource(CoreUiR.string.device_engine_unsupported, engine.reason)
                        is EngineAvailability.TemporarilyUnavailable ->
                            "Temporarily unavailable • ${engine.reason}"
                    },
                    "Capabilities: ${engine.descriptor.capabilities.joinToString()}",
                    (engine as? EngineAvailability.Available)?.let {
                        "Requested compute: ${it.requestedComputePreference}; effective: ${it.effectiveComputePreference}" +
                            (it.computeDetail?.let { detail -> "; runtime: $detail" } ?: "") +
                            "; threads: ${it.effectiveThreadCount ?: "not reported"}" +
                            (it.fallbackReason?.let { reason -> "; fallback: $reason" } ?: "")
                    } ?: "No active runtime is available.",
                    if (engine.descriptor.bundledRuntime) {
                        "Runtime is bundled with the app."
                    } else {
                        "Runtime is supplied by the system."
                    },
                ),
                isError = engine !is EngineAvailability.Available,
            )
        }
        state.diagnostics?.let { diagnostics ->
            item {
                DeviceInfoCard(
                    title = stringResource(CoreUiR.string.ui_copy_54),
                    lines = listOf(
                        "Model storage writable: ${diagnostics.modelDirectoryWritable}",
                        "Temporary storage: ${diagnostics.availableTemporaryBytes / 1024 / 1024} MiB available",
                        "Installed files valid: ${diagnostics.installedFilesValid}",
                        "Offline-ready capabilities: ${diagnostics.offlineReadyCapabilities.joinToString().ifBlank { "None" }}",
                    ) + diagnostics.detail,
                    isError = !diagnostics.modelDirectoryWritable || !diagnostics.installedFilesValid,
                )
            }
        }
        item {
            OutlinedButton(
                modifier = Modifier.padding(bottom = 24.dp),
                enabled = !state.refreshing,
                onClick = onRefresh,
            ) {
                Text(
                    stringResource(
                        if (state.refreshing) {
                            CoreUiR.string.device_refreshing
                        } else {
                            CoreUiR.string.device_refresh_diagnostics
                        },
                    ),
                )
            }
        }
    }
}
