package com.dmitriim.localaiplayground.feature.device.presentation

import com.dmitriim.localaiplayground.core.model.device.DeviceDiagnostics
import com.dmitriim.localaiplayground.core.model.engine.EngineAvailability

data class DeviceSnapshot(
    val deviceName: String,
    val androidVersion: String,
    val abis: String,
    val totalMemory: String,
    val availableMemory: String,
    val availableStorage: String,
    val batteryState: String,
    val thermalState: String,
    val cpuInfo: String,
)

data class DeviceUiState(
    val snapshot: DeviceSnapshot? = null,
    val engines: List<EngineAvailability> = emptyList(),
    val refreshing: Boolean = true,
    val interruptionMessage: String? = null,
    val diagnostics: DeviceDiagnostics? = null,
)
