package com.dmitriim.localailab.feature.assistant.impl.domain

import com.dmitriim.localailab.ai.api.chat.LlmChatMessage

/** Feature-owned formatting used when a runtime expects an already formatted prompt. */
internal fun interface CallerProvidedChatPromptFormatter {
    fun format(messages: List<LlmChatMessage>): String
}

internal object RoleLabeledChatPromptFormatter : CallerProvidedChatPromptFormatter {
    override fun format(messages: List<LlmChatMessage>): String = buildString {
        messages.forEachIndexed { index, message ->
            if (index > 0) append("\n\n")
            append(message.role.wireName)
            append(": ")
            append(message.content)
        }
        append("\n\nassistant:")
    }
}
