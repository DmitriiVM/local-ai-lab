package com.dmitriim.localailab.ai.api.stt

import com.dmitriim.localailab.core.model.engine.EngineId

/**
 * Concrete speech-to-text runtime contributed to the application engine set.
 *
 * [engineId] must be unique among packaged STT runtimes. Implementations own their loaded model
 * lifetime and must reject requests for a different engine ID.
 */
interface SpeechToTextRuntime : SpeechToTextEngine {
    val engineId: EngineId
}
