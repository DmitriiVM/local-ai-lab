package com.dmitriim.localailab.core.performance

import com.dmitriim.localailab.core.model.capability.AiCapability

interface InferenceProfiler {
    fun start(
        runId: String,
        capability: AiCapability,
        extendedTelemetry: Boolean = false,
    ): InferenceProfileSession
}
