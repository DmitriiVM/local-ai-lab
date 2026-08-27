package com.dmitriim.localailab.ai.api.stt

/**
 * Engine-neutral STT boundary. Callers currently submit bounded PCM segments after capture;
 * a runtime may decode those segments with either an offline or streaming recognizer.
 */
interface SpeechToTextEngine {
    val isLoaded: Boolean

    fun load(request: SpeechToTextLoadRequest): SpeechToTextLoadResult

    fun transcribe(request: SpeechToTextRequest): SpeechToTextResult

    fun cancel()

    fun unload()
}
