package com.dmitriim.localaiplayground.core.performance

import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import kotlinx.serialization.Serializable

@Serializable
data class BenchmarkPlan(
    val sessionId: String,
    val capability: AiCapability,
    val modelId: String,
    val modelDisplayName: String,
    val workloadFingerprint: String,
    val warmupIterations: Int = 2,
    val measuredIterations: Int = 10,
    val startupMode: BenchmarkStartupMode = BenchmarkStartupMode.WARM,
    val parametersJson: String,
)

@Serializable
enum class BenchmarkStartupMode { WARM, COLD }
