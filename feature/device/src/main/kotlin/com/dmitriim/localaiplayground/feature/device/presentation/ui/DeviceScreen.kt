package com.dmitriim.localaiplayground.feature.device.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.EngineAvailability
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions
import com.dmitriim.localaiplayground.feature.device.presentation.DeviceUiState

@Composable
fun DeviceScreen(
    state: DeviceUiState,
    onRefresh: () -> Unit,
    onRunFoundationLifecycleCheck: () -> Unit,
    onCancelFoundationLifecycleCheck: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(
            top = dimensions.topBarOverlayClearance,
            bottom = 112.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Device",
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
                    ),
                )
            }
        }
        state.interruptionMessage?.let { message ->
            item {
                DeviceInfoCard(
                    title = "Refresh interrupted",
                    lines = listOf(message),
                    isError = true,
                )
            }
        }
        item {
            FoundationLifecycleCard(
                operation = state.foundationOperation,
                onRun = onRunFoundationLifecycleCheck,
                onCancel = onCancelFoundationLifecycleCheck,
            )
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
                            "Available • ${engine.effectiveBackend} • ${engine.detail}"
                        is EngineAvailability.Unsupported -> "Unsupported • ${engine.reason}"
                        is EngineAvailability.TemporarilyUnavailable ->
                            "Temporarily unavailable • ${engine.reason}"
                    },
                    "Capabilities: ${engine.descriptor.capabilities.joinToString()}",
                    if (engine.descriptor.bundledRuntime) {
                        "Runtime is bundled with the app."
                    } else {
                        "Runtime is supplied by the system."
                    },
                ),
                isError = engine !is EngineAvailability.Available,
            )
        }
        item {
            OutlinedButton(
                modifier = Modifier.padding(bottom = 24.dp),
                enabled = !state.refreshing,
                onClick = onRefresh,
            ) {
                Text(if (state.refreshing) "Refreshing…" else "Refresh diagnostics")
            }
        }
    }
}
