package com.dmitriim.localaiplayground.ai.sherpa

data class StageZeroSttResult(
    val text: String,
    val durationMs: Long,
)

data class StageZeroTtsResult(
    val sampleRate: Int,
    val sampleCount: Int,
    val durationMs: Long,
)
