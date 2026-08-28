package com.dmitriim.localailab.ai.api.tts

/**
 * Engine-neutral facade for synthesizing one text request into PCM audio.
 *
 * Call [load] before [synthesize], and run all calls away from the Android main thread.
 * [onAudioChunk] receives ordered PCM chunks on the runtime's execution thread; returning false
 * requests that synthesis stop. [cancel] is best-effort and [unload] releases model resources.
 */
interface TextToSpeechEngine {
    /** Whether a synthesizer is currently loaded and ready to generate PCM audio. */
    val isLoaded: Boolean

    /** Loads or reuses the synthesizer selected by [request]. */
    fun load(request: TextToSpeechLoadRequest): TextToSpeechLoadResult

    /** Synthesizes one request and returns the complete generated PCM audio. */
    fun synthesize(
        request: TextToSpeechRequest,
        onAudioChunk: (FloatArray) -> Boolean,
    ): TextToSpeechResult

    /** Requests cancellation of an active synthesis. */
    fun cancel()

    /** Cancels active work if required and releases the loaded synthesizer. */
    fun unload()
}
