package com.dmitriim.localaiplayground.ai.api.stt

data class SpeechToTextRequest(
    val samples: FloatArray,
    val sampleRateHz: Int,
)

data class SpeechToTextResult(
    val text: String,
    val processingDurationMs: Long,
)
