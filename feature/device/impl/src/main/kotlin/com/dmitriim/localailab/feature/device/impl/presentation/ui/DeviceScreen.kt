package com.dmitriim.localailab.feature.device.impl.presentation.ui

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
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.engine.ComputePreference
import com.dmitriim.localailab.ai.api.engine.EngineAvailability
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.layout.LocalAppDimensions
import com.dmitriim.localailab.feature.device.impl.presentation.DeviceUiState

@Composable
fun DeviceScreen(
    state: DeviceUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = modifier
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
    contentPadding: PaddingValues,
    itemSpacing: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
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
                    stringResource(CoreUiR.string.device_abis, snapshot.abis),
                    stringResource(
                        CoreUiR.string.device_memory,
                        snapshot.availableMemory,
                        snapshot.totalMemory,
                    ),
                    stringResource(CoreUiR.string.device_app_storage_available, snapshot.availableStorage),
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
                stringResource(
                    CoreUiR.string.device_capabilities,
                    engine.descriptor.capabilities.displayNames(),
                ),
                engine.runtimeLine(),
                if (engine.descriptor.bundledRuntime) {
                    stringResource(CoreUiR.string.device_runtime_bundled)
                } else {
                    stringResource(CoreUiR.string.device_runtime_system)
                },
            ),
            isError = engine !is EngineAvailability.Available,
        )
    }
}

@Composable
private fun EngineAvailability.statusLine(): String = when (this) {
    is EngineAvailability.Available -> stringResource(
        CoreUiR.string.device_engine_available,
        effectiveComputePreference.displayName(),
        detail,
    )
    is EngineAvailability.Unsupported -> stringResource(CoreUiR.string.device_engine_unsupported, reason)
    is EngineAvailability.TemporarilyUnavailable -> stringResource(
        CoreUiR.string.device_engine_temporarily_unavailable,
        reason,
    )
}

@Composable
private fun EngineAvailability.runtimeLine(): String {
    val availability = this as? EngineAvailability.Available
        ?: return stringResource(CoreUiR.string.device_runtime_unavailable)
    var line = stringResource(
        CoreUiR.string.device_runtime_compute,
        availability.requestedComputePreference.displayName(),
        availability.effectiveComputePreference.displayName(),
    )
    availability.computeDetail?.let { detail ->
        line += stringResource(CoreUiR.string.device_runtime_detail, detail)
    }
    line += stringResource(
        CoreUiR.string.device_runtime_threads,
        availability.effectiveThreadCount?.toString()
            ?: stringResource(CoreUiR.string.device_runtime_threads_not_reported),
    )
    availability.fallbackReason?.let { reason ->
        line += stringResource(CoreUiR.string.device_runtime_fallback, reason)
    }
    return line
}

private fun LazyListScope.diagnosticsCard(state: DeviceUiState) {
    state.diagnostics?.let { diagnostics ->
        item {
            val offlineCapabilities = diagnostics.offlineReadyCapabilities.displayNames()
            DeviceInfoCard(
                title = stringResource(CoreUiR.string.ui_copy_54),
                lines = listOf(
                    stringResource(
                        CoreUiR.string.device_model_storage_writable,
                        diagnostics.modelDirectoryWritable,
                    ),
                    stringResource(
                        CoreUiR.string.device_temporary_storage,
                        diagnostics.availableTemporaryBytes / 1024 / 1024,
                    ),
                    stringResource(
                        CoreUiR.string.device_installed_files_valid,
                        diagnostics.installedFilesValid,
                    ),
                    stringResource(
                        CoreUiR.string.device_offline_ready_capabilities,
                        if (offlineCapabilities.isBlank()) {
                            stringResource(CoreUiR.string.device_none)
                        } else {
                            offlineCapabilities
                        },
                    ),
                ) + diagnostics.detail,
                isError = !diagnostics.modelDirectoryWritable || !diagnostics.installedFilesValid,
            )
        }
    }
}

@Composable
private fun ComputePreference.displayName(): String = when (this) {
    ComputePreference.AUTO -> stringResource(CoreUiR.string.compute_automatic)
    ComputePreference.CPU -> stringResource(CoreUiR.string.compute_cpu)
    ComputePreference.GPU -> stringResource(CoreUiR.string.compute_gpu)
    ComputePreference.NPU -> stringResource(CoreUiR.string.compute_npu)
    ComputePreference.SYSTEM_SERVICE -> stringResource(CoreUiR.string.compute_system_service)
}

@Composable
private fun Set<AiCapability>.displayNames(): String {
    val labels = mutableListOf<String>()
    for (capability in this) {
        labels += stringResource(capability.labelResource())
    }
    return labels.joinToString()
}

private fun AiCapability.labelResource(): Int = when (this) {
    AiCapability.CHAT -> CoreUiR.string.runs_capability_chat
    AiCapability.SPEECH_TO_TEXT -> CoreUiR.string.runs_capability_speech_to_text
    AiCapability.TEXT_TO_SPEECH -> CoreUiR.string.runs_capability_text_to_speech
    AiCapability.VOICE_ACTIVITY_DETECTION -> CoreUiR.string.runs_capability_voice_activity_detection
    AiCapability.VOICE_ASSISTANT -> CoreUiR.string.runs_capability_voice_assistant
}
