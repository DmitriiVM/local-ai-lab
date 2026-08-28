package com.dmitriim.localailab.ai.api.chat

/** Optional operation implemented by runtimes that format structured chat messages. */
fun interface LlmChatFormatter {
    fun format(messages: List<LlmChatMessage>): String
}
