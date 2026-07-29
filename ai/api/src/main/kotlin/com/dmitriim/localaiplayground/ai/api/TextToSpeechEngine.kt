package com.dmitriim.localaiplayground.ai.api

import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.core.model.ModelProfileId

/**
 * Engine-neutral TTS facade. The router keeps the selected backend warm until [unload] is
 * explicitly called by the owning screen, a model switch occurs, or Android requests memory.
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

/** A concrete runtime contributed into the application engine set. */
interface TextToSpeechBackend : AutoCloseable {
    val engineId: EngineId
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
    val engineId: EngineId,
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
    val speakerCount: Int?,
)

sealed interface TextToSpeechVoiceCondition {
    data class FixedSpeaker(val speakerId: Int) : TextToSpeechVoiceCondition

    /** App-private, mono PCM16 reference. External document URIs never cross this boundary. */
    data class ReferenceAudio(
        val referenceId: String,
        val displayName: String,
        val pcmFilePath: String,
        val sampleRateHz: Int,
    ) : TextToSpeechVoiceCondition
}

data class TextToSpeechRequest(
    val text: String,
    val languageCode: String,
    val voice: TextToSpeechVoiceCondition,
    val speed: Float,
    val sentenceSilenceScale: Float,
)

data class TextToSpeechStageMetrics(
    val conditioningDurationMs: Long? = null,
    val tokenGenerationDurationMs: Long? = null,
    val decoderDurationMs: Long? = null,
    val generatedTokenCount: Int? = null,
    val conditioningCacheHit: Boolean? = null,
    val peakProcessPssBytes: Long? = null,
    val availableDeviceMemoryBytes: Long? = null,
)

class TextToSpeechResult(
    val samples: FloatArray,
    val sampleRateHz: Int,
    val stageMetrics: TextToSpeechStageMetrics = TextToSpeechStageMetrics(),
)
