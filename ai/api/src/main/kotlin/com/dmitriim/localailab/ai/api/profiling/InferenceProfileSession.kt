package com.dmitriim.localailab.ai.api.profiling

interface InferenceProfileSession {
    suspend fun <T> trace(phase: InferencePhase, block: suspend () -> T): T

    fun finish(): InferenceTelemetry
}
