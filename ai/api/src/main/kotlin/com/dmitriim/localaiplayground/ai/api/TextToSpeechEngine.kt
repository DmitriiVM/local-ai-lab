package com.dmitriim.localaiplayground.ai.api

import com.dmitriim.localaiplayground.core.model.ModelProfileId

/**
 * Engine-neutral boundary for local text-to-speech. Incremental PCM is delivered
 * synchronously so the caller can apply bounded backpressure to native synthesis.
 */
interface TextToSpeechEngine : AutoCloseable {
    val isLoaded: Boolean

    fun load(request: TextToSpeechLoadRequest): TextToSpeechLoadResult

    fun synthesize(
        request: TextToSpeechRequest,
        onAudioChunk: (FloatArray) -> Boolean,
    ): TextToSpeechResult

    fun cancel()

    fun unload()

    override fun close() = unload()
}

data class TextToSpeechLoadRequest(
    val profileType: ModelProfileId,
    val modelDirectory: String,
    /** Zero selects an engine-safe default. */
    val threadCount: Int = 0,
)

data class TextToSpeechLoadResult(
    val effectiveThreadCount: Int,
    val loadDurationMs: Long,
    val coldStart: Boolean,
    val sampleRateHz: Int,
    val speakerCount: Int,
)

data class TextToSpeechRequest(
    val text: String,
    val languageCode: String,
    val speakerId: Int,
    val speed: Float,
    val sentenceSilenceScale: Float,
)

class TextToSpeechResult(
    val samples: FloatArray,
    val sampleRateHz: Int,
)
