package com.dmitriim.localailab.ai.api.tts

import com.dmitriim.localailab.core.model.engine.EngineId

/** A concrete TTS runtime contributed into the application engine set. */
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
