package com.dmitriim.localaiplayground.ai.api.llm

/** Optional operation implemented by runtimes that provide exact prompt token counts. */
fun interface LlmTokenCounter {
    fun countTokens(prompt: String): Int
}
