package com.dmitriim.localailab.feature.device.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.engine.EngineAvailability
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.layout.LocalAppDimensions
import com.dmitriim.localailab.feature.device.presentation.DeviceUiState

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

    DeviceScreenContent(
        state = state,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .then(systemNavigationPadding)
            .padding(horizontal = dimensions.screenPadding),
        contentPadding = PaddingValues(
            top = dimensions.topBarOverlayClearance + 16.dp,
            bottom = 24.dp + dimensions.bottomNavigationOverlayClearance,
        ),
        itemSpacing = dimensions.itemSpacing,
    )
}

@Composable
private fun DeviceScreenContent(
    state: DeviceUiState,
    onRefresh: () -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
    itemSpacing: androidx.compose.ui.unit.Dp,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
    ) {
        deviceHeading()
        deviceSnapshot(state)
        interruption(state)
        engineCards(state)
        diagnosticsCard(state)
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

private fun LazyListScope.deviceHeading() {
    item {
        Text(
            text = stringResource(CoreUiR.string.ui_copy_52),
            modifier = Modifier.padding(top = 20.dp),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

private fun LazyListScope.deviceSnapshot(state: DeviceUiState) {
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
}

private fun LazyListScope.interruption(state: DeviceUiState) {
    state.interruptionMessage?.let { message ->
        item {
            DeviceInfoCard(
                title = stringResource(CoreUiR.string.ui_copy_53),
                lines = listOf(message),
                isError = true,
            )
        }
    }
}

private fun LazyListScope.engineCards(state: DeviceUiState) {
    items(state.engines.size, key = { state.engines[it].descriptor.id.value }) { index ->
        val engine = state.engines[index]
        DeviceInfoCard(
            title = engine.descriptor.displayName,
            lines = listOf(
                engine.statusLine(),
                "Capabilities: ${engine.descriptor.capabilities.joinToString()}",
                engine.runtimeLine(),
                if (engine.descriptor.bundledRuntime) "Runtime is bundled with the app." else "Runtime is supplied by the system.",
            ),
            isError = engine !is EngineAvailability.Available,
        )
    }
}

@Composable
private fun EngineAvailability.statusLine(): String = when (this) {
    is EngineAvailability.Available -> "Available • $effectiveComputePreference • $detail"
    is EngineAvailability.Unsupported -> stringResource(CoreUiR.string.device_engine_unsupported, reason)
    is EngineAvailability.TemporarilyUnavailable -> "Temporarily unavailable • $reason"
}

private fun EngineAvailability.runtimeLine(): String = (this as? EngineAvailability.Available)?.let {
    "Requested compute: $requestedComputePreference; effective: $effectiveComputePreference" +
        (computeDetail?.let { detail -> "; runtime: $detail" } ?: "") +
        "; threads: ${effectiveThreadCount ?: "not reported"}" +
        (fallbackReason?.let { reason -> "; fallback: $reason" } ?: "")
} ?: "No active runtime is available."

private fun LazyListScope.diagnosticsCard(state: DeviceUiState) {
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
}
