package com.dmitriim.localailab.core.voice.stt

import com.dmitriim.localailab.core.performance.profiling.InferenceTelemetry

sealed interface SpeechTranscriptionEvent {
    data class Prepared(
        val modelName: String,
        val loadDurationMs: Long,
        val effectiveThreadCount: Int,
    ) : SpeechTranscriptionEvent

    data class Completed(
        val transcript: String,
        val metrics: SpeechTranscriptionMetrics,
    ) : SpeechTranscriptionEvent
}

data class SpeechTranscriptionMetrics(
    val audioDurationMs: Long,
    val processingDurationMs: Long,
    val timeToFinalMs: Long,
    val realTimeFactor: Double?,
    val segmentCount: Int,
    val loadDurationMs: Long,
    val effectiveThreadCount: Int,
    val telemetry: InferenceTelemetry? = null,
)
