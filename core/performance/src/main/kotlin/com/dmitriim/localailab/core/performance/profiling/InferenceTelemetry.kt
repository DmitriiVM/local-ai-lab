package com.dmitriim.localailab.core.performance.profiling

import com.dmitriim.localailab.ai.api.capability.AiCapability
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InferenceTelemetry(
    val runId: String,
    val capability: AiCapability,
    @SerialName("traceActive")
    val systemTraceEnabled: Boolean,
    val wallDurationMs: Long,
    val phaseDurations: List<InferencePhaseDuration>,
    val resources: InferenceResourceMetrics? = null,
    val device: InferenceDeviceSnapshot? = null,
)

@Serializable
data class InferencePhaseDuration(
    val phase: InferencePhase,
    val durationMs: Long,
)

@Serializable
data class InferenceResourceMetrics(
    val processCpuTimeMs: Long,
    val averageProcessCpuPercent: Double?,
    val peakProcessCpuPercent: Double?,
    val startPssBytes: Long?,
    val endPssBytes: Long?,
    val peakPssBytes: Long?,
    val availableMemoryStartBytes: Long?,
    val availableMemoryEndBytes: Long?,
    val batteryEnergyDeltaNwh: Long?,
    val batteryChargeDeltaUah: Long?,
    val averageBatteryCurrentUa: Double?,
    val batteryMeasurementsAvailable: Boolean,
    val powerSaveMode: Boolean,
    val thermalStatusStart: Int?,
    val thermalStatusEnd: Int?,
    val thermalHeadroomStart: Float?,
    val thermalHeadroomEnd: Float?,
)

@Serializable
data class InferenceDeviceSnapshot(
    val manufacturer: String,
    val model: String,
    val hardware: String,
    val socManufacturer: String?,
    val socModel: String?,
    val sdkInt: Int,
    val abis: List<String>,
    val availableProcessors: Int,
)
