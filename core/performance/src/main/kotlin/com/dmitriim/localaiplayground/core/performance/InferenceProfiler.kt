package com.dmitriim.localaiplayground.core.performance

import com.dmitriim.localaiplayground.core.model.capability.AiCapability

interface InferenceProfiler {
    fun start(
        runId: String,
        capability: AiCapability,
        extendedTelemetry: Boolean = false,
    ): InferenceProfileSession
}
