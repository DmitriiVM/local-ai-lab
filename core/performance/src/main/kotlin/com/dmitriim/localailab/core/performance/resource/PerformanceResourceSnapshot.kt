package com.dmitriim.localailab.core.performance.resource

data class PerformanceResourceSnapshot(
    val elapsedRealtimeMs: Long,
    val processCpuTimeMs: Long,
    val pssBytes: Long?,
    val availableMemoryBytes: Long?,
    val batteryEnergyNwh: Long?,
    val batteryChargeUah: Long?,
    val batteryCurrentUa: Int?,
    val powerSaveMode: Boolean,
    val thermalStatus: Int?,
    val thermalHeadroom: Float?,
)
