package com.dmitriim.localaiplayground.ai.api

import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.core.model.ModelProfileId
import kotlinx.coroutines.flow.StateFlow

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

/** A locally available voice exposed by the operating system's text-to-speech service. */
data class SystemTextToSpeechVoice(
    val id: String,
    val displayName: String,
    val languageTag: String,
    val description: String?,
)

/** Discovers on-device voices supplied by Android without coupling features to the platform module. */
interface SystemTextToSpeechSupport {
    val voices: StateFlow<List<SystemTextToSpeechVoice>>

    /** Initializes the platform engine and refreshes [voices]. This call may block. */
    fun refresh()
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
    /** Zero when the backend reports its output format only after synthesis begins. */
    val sampleRateHz: Int,
    val speakerCount: Int?,
)

sealed interface TextToSpeechVoiceCondition {
    data class FixedSpeaker(val speakerId: Int) : TextToSpeechVoiceCondition

    data class PlatformVoice(val voiceId: String) : TextToSpeechVoiceCondition

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
