package com.dmitriim.localaiplayground.ai.api

import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.core.model.ModelFileRole
import com.dmitriim.localaiplayground.core.model.ModelProfileId

/**
 * Engine-neutral STT boundary. Callers currently submit bounded PCM segments after capture;
 * a backend may decode those segments with either an offline or streaming recognizer.
 */
interface SpeechToTextEngine : AutoCloseable {
    val isLoaded: Boolean

    fun load(request: SpeechToTextLoadRequest): SpeechToTextLoadResult

    fun transcribe(request: SpeechToTextRequest): SpeechToTextResult

    fun cancel()

    fun unload()

    override fun close() = unload()
}

/** A concrete STT runtime contributed into the application engine set. */
interface SpeechToTextBackend : AutoCloseable {
    val engineId: EngineId
    val isLoaded: Boolean
    fun load(request: SpeechToTextLoadRequest): SpeechToTextLoadResult
    fun transcribe(request: SpeechToTextRequest): SpeechToTextResult
    fun cancel()
    fun unload()
    override fun close() = unload()
}

/** Reports whether an operating-system STT backend can be offered on the current device. */
interface SystemSpeechToTextSupport {
    val isOnDeviceRecognizerAvailable: Boolean
}

data class SpeechToTextLoadRequest(
    val engineId: EngineId,
    val profileType: ModelProfileId,
    val modelDirectory: String,
    val files: Map<ModelFileRole, String>,
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
