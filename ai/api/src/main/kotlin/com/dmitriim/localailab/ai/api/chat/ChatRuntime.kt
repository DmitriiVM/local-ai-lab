package com.dmitriim.localailab.ai.api.chat

import com.dmitriim.localailab.core.model.engine.EngineId

/** A concrete chat runtime contributed to the application engine set. */
interface ChatRuntime : ChatExecution {
    val engineId: EngineId
    val capabilities: LlmEngineCapabilities
}
