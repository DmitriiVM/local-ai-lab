package com.dmitriim.localailab.core.performance.profiling

import com.dmitriim.localailab.ai.api.capability.AiCapability

interface InferenceProfiler {
    fun start(
        runId: String,
        capability: AiCapability,
        collectResourceTelemetry: Boolean = false,
    ): InferenceProfileSession
}
