package com.dmitriim.localailab.ai.api.chat

import com.dmitriim.localailab.core.model.engine.EngineId

/**
 * Concrete local-chat runtime contributed to the application engine set.
 *
 * [engineId] must be unique among packaged chat runtimes. [capabilities] is a stable declaration:
 * every optional operation or setting it advertises must be implemented by this runtime.
 */
interface ChatRuntime : ChatExecution {
    val engineId: EngineId
    val capabilities: LlmEngineCapabilities
}
