package com.dmitriim.localailab.feature.assistant.impl.presentation.state

import java.util.UUID

enum class ChatMessageRole { USER, ASSISTANT }

data class ChatMessage(
    val id: String,
    val role: ChatMessageRole,
    val content: String,
    val streaming: Boolean = false,
    val failed: Boolean = false,
) {
    companion object {
        fun user(content: String) = ChatMessage(UUID.randomUUID().toString(), ChatMessageRole.USER, content)

        fun assistant(
            id: String,
            content: String,
            streaming: Boolean,
        ) = ChatMessage(id, ChatMessageRole.ASSISTANT, content, streaming)
    }
}
