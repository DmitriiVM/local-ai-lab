package com.dmitriim.localaiplayground.feature.tts.domain

import com.dmitriim.localaiplayground.core.audio.output.model.GeneratedAudioFile

sealed interface SpeechSynthesisEvent {
    data class Prepared(
        val modelName: String,
        val loadDurationMs: Long,
        val effectiveThreadCount: Int,
        val sampleRateHz: Int,
        val speakerCount: Int,
    ) : SpeechSynthesisEvent

    data class Synthesized(
        val output: GeneratedAudioFile,
        val synthesisDurationMs: Long,
    ) : SpeechSynthesisEvent

    data class Completed(
        val output: GeneratedAudioFile,
        val metrics: SpeechSynthesisMetrics,
    ) : SpeechSynthesisEvent
}

data class SpeechSynthesisMetrics(
    val timeToFirstChunkMs: Long?,
    val timeToFirstWriteMs: Long?,
    val timeToFirstPresentationMs: Long?,
    val synthesisDurationMs: Long,
    val generatedAudioDurationMs: Long,
    val realTimeFactor: Double?,
    val sampleRateHz: Int,
    val playbackUnderrunCount: Int,
    val loadDurationMs: Long,
    val effectiveThreadCount: Int,
)