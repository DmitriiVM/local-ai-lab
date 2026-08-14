package com.dmitriim.localailab.core.performance

import kotlinx.serialization.Serializable

@Serializable
data class BenchmarkIterationResult(
    val runId: String,
    val iteration: Int,
    val latencyMs: Long,
    val throughputPerSecond: Double? = null,
    val outputUnits: Int? = null,
    val telemetry: InferenceTelemetry,
    val warning: String? = null,
)
