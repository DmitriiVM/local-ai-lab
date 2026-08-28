package com.dmitriim.localailab.ai.api.stt

class SpeechToTextRequest(
    val samples: FloatArray,
    val sampleRateHz: Int,
)

data class SpeechToTextResult(val text: String, val processingDurationMs: Long)
