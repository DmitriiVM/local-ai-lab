package com.dmitriim.localailab.ai.api.llm

import com.dmitriim.localailab.core.model.engine.EngineId

/** A concrete LLM runtime contributed to the application engine set. */
interface LlmRuntime : LlmEngine {
    val engineId: EngineId
    val capabilities: LlmEngineCapabilities
}
