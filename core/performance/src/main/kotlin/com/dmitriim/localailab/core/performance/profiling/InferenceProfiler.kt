package com.dmitriim.localailab.core.performance.profiling

import com.dmitriim.localailab.core.model.capability.AiCapability

interface InferenceProfiler {
    fun start(
        runId: String,
        capability: AiCapability,
        collectResourceTelemetry: Boolean = false,
    ): InferenceProfileSession
}
