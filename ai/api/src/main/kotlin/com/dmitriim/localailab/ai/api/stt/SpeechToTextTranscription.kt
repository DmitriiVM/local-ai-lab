package com.dmitriim.localailab.ai.api.stt

/**
 * One complete PCM audio segment to transcribe.
 *
 * [samples] contains normalized mono floating-point PCM samples. The runtime must not retain or
 * mutate this caller-owned array after [SpeechToTextEngine.transcribe] returns.
 */
class SpeechToTextRequest(
    val samples: FloatArray,
    val sampleRateHz: Int,
)

/** Final transcript and the runtime's processing duration in milliseconds. */
data class SpeechToTextResult(val text: String, val processingDurationMs: Long)
