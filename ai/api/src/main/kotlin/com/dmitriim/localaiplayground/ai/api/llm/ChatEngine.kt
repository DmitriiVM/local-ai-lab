package com.dmitriim.localaiplayground.ai.api.llm

import com.dmitriim.localaiplayground.core.model.engine.EngineId

/** The Stage 3 feature-facing local chat engine. */
interface ChatEngine : LlmEngine {
    fun capabilitiesFor(engineId: EngineId): LlmEngineCapabilities?
    fun activeChatFormatter(): LlmChatFormatter?
    fun activeTokenCounter(): LlmTokenCounter?
}
