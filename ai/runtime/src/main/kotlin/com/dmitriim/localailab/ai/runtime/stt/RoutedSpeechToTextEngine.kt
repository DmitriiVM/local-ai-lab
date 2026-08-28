package com.dmitriim.localailab.ai.runtime.stt

import com.dmitriim.localailab.ai.api.stt.SpeechToTextEngine
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadResult
import com.dmitriim.localailab.ai.api.stt.SpeechToTextRequest
import com.dmitriim.localailab.ai.api.stt.SpeechToTextResult
import com.dmitriim.localailab.ai.api.stt.SpeechToTextRuntime
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/** Routes STT requests to the runtime selected by engine ID and owns its active lifetime. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RoutedSpeechToTextEngine(runtimes: Set<SpeechToTextRuntime>) : SpeechToTextEngine {
    private val lock = Any()
    private val byEngineId = runtimes.associateBy(SpeechToTextRuntime::engineId).also {
        require(it.size == runtimes.size) {
            "More than one STT runtime declares the same engine ID."
        }
    }
    private var active: SpeechToTextRuntime? = null

    override val isLoaded: Boolean
        get() = synchronized(lock) { active?.isLoaded == true }

    override fun load(request: SpeechToTextLoadRequest): SpeechToTextLoadResult {
        val runtime = synchronized(lock) {
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
        return runtime.load(request)
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
