package com.dmitriim.localailab.ai.api.tts

/**
 * Engine-neutral TTS facade. The router keeps the selected backend warm until [unload] is
 * explicitly called by the owning screen, a model switch occurs, or Android requests memory.
 */
interface TextToSpeechEngine {
    val isLoaded: Boolean

    fun load(request: TextToSpeechLoadRequest): TextToSpeechLoadResult

    fun synthesize(
        request: TextToSpeechRequest,
        onAudioChunk: (FloatArray) -> Boolean,
    ): TextToSpeechResult

    fun cancel()

    fun unload()

}
