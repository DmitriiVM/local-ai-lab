package com.dmitriim.localaiplayground.ai.api.stt

import com.dmitriim.localaiplayground.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RoutedSpeechToTextEngine(backends: Set<SpeechToTextBackend>) : SpeechToTextEngine {
    private val lock = Any()
    private val byEngineId = backends.associateBy(SpeechToTextBackend::engineId).also {
        require(it.size == backends.size) {
            "More than one STT backend declares the same engine ID."
        }
    }
    private var active: SpeechToTextBackend? = null

    override val isLoaded: Boolean
        get() = synchronized(lock) { active?.isLoaded == true }

    override fun load(request: SpeechToTextLoadRequest): SpeechToTextLoadResult {
        val backend = synchronized(lock) {
            val selected = requireNotNull(byEngineId[request.engineId]) {
                "No packaged speech-to-text engine supports ${request.engineId.value}."
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

    override fun transcribe(request: SpeechToTextRequest): SpeechToTextResult = synchronized(lock) { checkNotNull(active) { "Load a speech model before transcription." } }
        .transcribe(request)

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
}
