package com.dmitriim.localailab.feature.assistant.impl.presentation.state

internal fun AssistantUiState.replaceAssistantText(
    id: String,
    text: String,
    append: Boolean,
): AssistantUiState = copy(
    messages = messages.map { message ->
        if (message.id == id) {
            message.copy(content = if (append) message.content + text else text, streaming = append)
        } else {
            message
        }
    },
)
