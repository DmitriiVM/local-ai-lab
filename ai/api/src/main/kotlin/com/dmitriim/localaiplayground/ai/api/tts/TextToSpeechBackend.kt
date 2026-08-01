package com.dmitriim.localaiplayground.ai.api.tts

import com.dmitriim.localaiplayground.core.model.engine.EngineId

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
