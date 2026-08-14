package com.dmitriim.localailab.core.performance

interface InferenceProfileSession {
    suspend fun <T> trace(phase: InferencePhase, block: suspend () -> T): T

    fun finish(): InferenceTelemetry
}
