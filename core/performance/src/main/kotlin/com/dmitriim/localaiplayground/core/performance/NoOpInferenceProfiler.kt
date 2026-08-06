package com.dmitriim.localaiplayground.core.performance

import android.os.SystemClock
import com.dmitriim.localaiplayground.core.model.capability.AiCapability

/** Lightweight fallback for direct construction outside the Android dependency graph. */
object NoOpInferenceProfiler : InferenceProfiler {
    override fun start(
        runId: String,
        capability: AiCapability,
        extendedTelemetry: Boolean,
    ): InferenceProfileSession = Session(runId, capability)

    private class Session(
        private val runId: String,
        private val capability: AiCapability,
    ) : InferenceProfileSession {
        private val startedAt = SystemClock.elapsedRealtime()
        private val phaseDurations = mutableListOf<InferencePhaseDuration>()
        private var telemetry: InferenceTelemetry? = null

        override suspend fun <T> trace(phase: InferencePhase, block: suspend () -> T): T {
            val phaseStartedAt = SystemClock.elapsedRealtime()
            return try {
                block()
            } finally {
                phaseDurations += InferencePhaseDuration(
                    phase = phase,
                    durationMs = SystemClock.elapsedRealtime() - phaseStartedAt,
                )
            }
        }

        override fun finish(): InferenceTelemetry = telemetry ?: InferenceTelemetry(
            runId = runId,
            capability = capability,
            traceActive = false,
            wallDurationMs = SystemClock.elapsedRealtime() - startedAt,
            phaseDurations = phaseDurations.toList(),
        ).also { telemetry = it }
    }
}
