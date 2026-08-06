package com.dmitriim.localaiplayground.core.performance

import kotlinx.serialization.Serializable

@Serializable
data class BenchmarkSessionSummary(
    val completedIterations: Int,
    val medianLatencyMs: Long?,
    val p95LatencyMs: Long?,
    val minimumLatencyMs: Long?,
    val maximumLatencyMs: Long?,
    val medianThroughputPerSecond: Double?,
    val totalBatteryEnergyDeltaNwh: Long?,
    val warning: String? = null,
)
