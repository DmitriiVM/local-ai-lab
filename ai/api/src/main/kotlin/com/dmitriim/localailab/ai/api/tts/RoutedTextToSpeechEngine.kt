package com.dmitriim.localailab.ai.api.tts

import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Converts the runtime multibinding into a stable engine-id lookup and owns the warm runtime
 * lifetime. A switch is transactional: the previous runtime is cancelled and unloaded first.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RoutedTextToSpeechEngine(runtimes: Set<TextToSpeechRuntime>) : TextToSpeechEngine {
    private val lock = Any()
    private val byEngineId = runtimes.associateBy(TextToSpeechRuntime::engineId).also { indexed ->
        require(indexed.size == runtimes.size) {
            "More than one TTS runtime declares the same engine ID."
        }
    }
    private var active: TextToSpeechRuntime? = null

    override val isLoaded: Boolean
        get() = synchronized(lock) { active?.isLoaded == true }

    override fun load(request: TextToSpeechLoadRequest): TextToSpeechLoadResult {
        val runtime = synchronized(lock) {
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
        return runtime.load(request)
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
