package com.dmitriim.localaiplayground.ai.api.llm

import com.dmitriim.localaiplayground.core.model.engine.EngineId

/** A concrete LLM runtime contributed to the application engine set. */
interface LlmRuntime : LlmEngine {
    val engineId: EngineId
    val capabilities: LlmEngineCapabilities
}
