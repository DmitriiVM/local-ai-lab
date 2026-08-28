package com.dmitriim.localailab.ai.api.chat

/**
 * Optional runtime operation that converts structured messages into that runtime's prompt form.
 *
 * Implemented only when [LlmChatTemplateHandling.ENGINE_FORMATS_MESSAGES] is declared. The
 * returned string is opaque to callers and must be passed unchanged to [ChatExecution.generate].
 */
fun interface LlmChatFormatter {
    /** Formats the complete retained conversation, including the final user message. */
    fun format(messages: List<LlmChatMessage>): String
}
