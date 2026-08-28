package com.dmitriim.localailab.ai.api.chat

/**
 * Optional operation implemented by runtimes that provide exact prompt token counts.
 *
 * The prompt must be the exact string passed to [ChatExecution.generate]; counts for individual
 * messages are not interchangeable with the count of a formatted prompt.
 */
fun interface LlmTokenCounter {
    fun countTokens(prompt: String): Int
}
