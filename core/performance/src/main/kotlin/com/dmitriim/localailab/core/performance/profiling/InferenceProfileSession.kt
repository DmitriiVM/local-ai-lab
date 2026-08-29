package com.dmitriim.localailab.core.performance.profiling

interface InferenceProfileSession {
    suspend fun <T> trace(phase: InferencePhase, block: suspend () -> T): T

    fun finish(): InferenceTelemetry
}
