package com.dmitriim.localaiplayground.ai.api

import com.dmitriim.localaiplayground.core.model.ModelProfileId

/**
 * Engine-neutral boundary for offline STT. The current Whisper profile is an
 * offline recognizer, so callers submit bounded PCM segments after capture ends.
 */
interface SpeechToTextEngine : AutoCloseable {
    val isLoaded: Boolean

    fun load(request: SpeechToTextLoadRequest): SpeechToTextLoadResult

    fun transcribe(request: SpeechToTextRequest): SpeechToTextResult

    fun cancel()

    fun unload()

    override fun close() = unload()
}

data class SpeechToTextLoadRequest(
    val profileType: ModelProfileId,
    val modelDirectory: String,
    val languageCode: String,
    /** Zero selects an engine-safe default. */
    val threadCount: Int = 0,
)

data class SpeechToTextLoadResult(
    val effectiveThreadCount: Int,
    val loadDurationMs: Long,
    val coldStart: Boolean,
)

data class SpeechToTextRequest(
    val samples: FloatArray,
    val sampleRateHz: Int,
)

data class SpeechToTextResult(
    val text: String,
    val processingDurationMs: Long,
)
