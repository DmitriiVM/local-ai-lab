package com.dmitriim.localailab.ai.api.llm

import com.dmitriim.localailab.core.model.engine.EngineId

interface ChatEngine : LlmEngine {
    fun capabilitiesFor(engineId: EngineId): LlmEngineCapabilities?
    fun activeChatFormatter(): LlmChatFormatter?
    fun activeTokenCounter(): LlmTokenCounter?
}
