package com.dmitriim.localailab.ai.api.stt

import com.dmitriim.localailab.core.model.engine.EngineId

/** A concrete STT runtime contributed into the application engine set. */
interface SpeechToTextRuntime {
    val engineId: EngineId
    val isLoaded: Boolean
    fun load(request: SpeechToTextLoadRequest): SpeechToTextLoadResult
    fun transcribe(request: SpeechToTextRequest): SpeechToTextResult
    fun cancel()
    fun unload()
}
