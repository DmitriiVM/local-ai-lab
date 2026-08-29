package com.dmitriim.localailab.core.performance.benchmark

import com.dmitriim.localailab.ai.api.capability.AiCapability

/** Stable, exportable configuration identity for a capability-specific benchmark workload. */
interface BenchmarkWorkloadConfiguration {
    val capability: AiCapability
    val modelId: String
}
