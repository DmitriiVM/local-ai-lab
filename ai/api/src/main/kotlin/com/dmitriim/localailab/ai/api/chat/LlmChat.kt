package com.dmitriim.localailab.ai.api.chat

data class LlmChatMessage(val role: LlmChatRole, val content: String)

enum class LlmChatRole(val wireName: String) {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
}
