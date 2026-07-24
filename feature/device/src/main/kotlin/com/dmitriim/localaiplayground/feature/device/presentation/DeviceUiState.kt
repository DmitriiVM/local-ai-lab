package com.dmitriim.localaiplayground.feature.device.presentation

import com.dmitriim.localaiplayground.core.model.EngineAvailability
import com.dmitriim.localaiplayground.core.result.OperationState

data class DeviceSnapshot(
    val deviceName: String,
    val androidVersion: String,
    val abis: String,
    val totalMemory: String,
    val availableMemory: String,
    val availableStorage: String,
)

data class DeviceUiState(
    val snapshot: DeviceSnapshot? = null,
    val engines: List<EngineAvailability> = emptyList(),
    val refreshing: Boolean = true,
    val interruptionMessage: String? = null,
    val foundationOperation: OperationState<Unit> = OperationState.Idle,
)
