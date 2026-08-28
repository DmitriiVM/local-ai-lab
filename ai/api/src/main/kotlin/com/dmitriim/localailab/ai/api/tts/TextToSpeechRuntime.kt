package com.dmitriim.localailab.ai.api.tts

import com.dmitriim.localailab.core.model.engine.EngineId

/**
 * Concrete text-to-speech runtime contributed to the application engine set.
 *
 * [engineId] must be unique among packaged TTS runtimes. Implementations own their loaded model
 * lifetime and must reject requests for a different engine ID.
 */
interface TextToSpeechRuntime {
    val engineId: EngineId
    val isLoaded: Boolean

    fun load(request: TextToSpeechLoadRequest): TextToSpeechLoadResult

    fun synthesize(
        request: TextToSpeechRequest,
        onAudioChunk: (FloatArray) -> Boolean,
    ): TextToSpeechResult

    fun cancel()
    fun unload()
}
