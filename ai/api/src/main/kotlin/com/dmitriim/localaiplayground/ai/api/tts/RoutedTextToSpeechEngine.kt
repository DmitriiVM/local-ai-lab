package com.dmitriim.localaiplayground.ai.api.tts

import com.dmitriim.localaiplayground.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Converts the backend multibinding into a stable engine-id lookup and owns the warm backend
 * lifetime. A switch is transactional: the previous backend is cancelled and unloaded first.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RoutedTextToSpeechEngine(backends: Set<TextToSpeechBackend>) : TextToSpeechEngine {
    private val lock = Any()
    private val byEngineId = backends.associateBy(TextToSpeechBackend::engineId).also { indexed ->
        require(indexed.size == backends.size) {
            "More than one TTS backend declares the same engine ID."
        }
    }
    private var active: TextToSpeechBackend? = null

    override val isLoaded: Boolean
        get() = synchronized(lock) { active?.isLoaded == true }

    override fun load(request: TextToSpeechLoadRequest): TextToSpeechLoadResult {
        val backend = synchronized(lock) {
            val selected = requireNotNull(byEngineId[request.engineId]) {
                "No packaged text-to-speech engine supports ${request.engineId.value}."
            }
            if (active !== selected) {
                active?.cancel()
                active?.unload()
                active = selected
            }
            selected
        }
        return backend.load(request)
    }

    override fun synthesize(
        request: TextToSpeechRequest,
        onAudioChunk: (FloatArray) -> Boolean,
    ): TextToSpeechResult = synchronized(lock) {
        checkNotNull(active) { "Load a voice model before synthesis." }
    }.synthesize(request, onAudioChunk)

    override fun cancel() {
        synchronized(lock) { active }?.cancel()
    }

    override fun unload() {
        synchronized(lock) {
            active?.cancel()
            active?.unload()
            active = null
        }
    }

    fun unloadForMemoryPressure() = unload()
}
