package com.dmitriim.localailab.ai.api.stt

/**
 * Engine-neutral boundary for transcribing bounded, captured PCM audio.
 *
 * Call [load] successfully before [transcribe]. Calls must run away from the Android main thread.
 * [cancel] is best-effort and does not unload the model; [unload] cancels active work if needed
 * and releases the selected runtime's resources.
 */
interface SpeechToTextEngine {
    /** Whether a recognizer is currently loaded and ready to transcribe. */
    val isLoaded: Boolean

    /** Loads or reuses the recognizer selected by [request]. */
    fun load(request: SpeechToTextLoadRequest): SpeechToTextLoadResult

    /** Transcribes one complete PCM segment and returns its final text and processing duration. */
    fun transcribe(request: SpeechToTextRequest): SpeechToTextResult

    /** Requests cancellation of an active transcription. */
    fun cancel()

    /** Releases the loaded recognizer and any native resources it owns. */
    fun unload()
}
