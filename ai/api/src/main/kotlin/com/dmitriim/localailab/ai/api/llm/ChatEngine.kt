package com.dmitriim.localailab.ai.api.llm

import com.dmitriim.localailab.core.model.engine.EngineId

/**
 * Application-facing LLM facade that routes chat work to a packaged runtime.
 *
 * It additionally exposes selected-runtime capabilities needed before prompt preparation and
 * optional operations available only after a model is loaded.
 */
interface ChatEngine : LlmEngine {
    fun capabilitiesFor(engineId: EngineId): LlmEngineCapabilities?
    fun activeChatFormatter(): LlmChatFormatter?
    fun activeTokenCounter(): LlmTokenCounter?
}
