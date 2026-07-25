package com.dmitriim.localaiplayground.core.audio.output.model

data class GeneratedAudioFile(
    val filePath: String,
    val sampleRateHz: Int,
    val sampleCount: Int,
    val createdAtEpochMs: Long,
) {
    val durationMs: Long get() = sampleCount.toLong() * 1_000L / sampleRateHz
    val displayName: String get() = "local-ai-speech.wav"
}
