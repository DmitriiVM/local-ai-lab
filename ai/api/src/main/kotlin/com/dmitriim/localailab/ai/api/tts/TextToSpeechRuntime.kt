package com.dmitriim.localailab.ai.api.tts

import com.dmitriim.localailab.core.model.engine.EngineId

/**
 * Concrete text-to-speech runtime contributed to the application engine set.
 *
 * [engineId] must be unique among packaged TTS runtimes. Implementations own their loaded model
 * lifetime and must reject requests for a different engine ID.
 */
interface TextToSpeechRuntime : TextToSpeechEngine {
    val engineId: EngineId
}
