package com.dmitriim.localaiplayground.ai.api.llm

/** Optional operation implemented by runtimes that format structured chat messages. */
fun interface LlmChatFormatter {
    fun format(messages: List<LlmChatMessage>): String
}
