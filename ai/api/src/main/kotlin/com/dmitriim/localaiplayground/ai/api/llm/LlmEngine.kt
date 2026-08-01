package com.dmitriim.localaiplayground.ai.api.llm

/** Engine-neutral local chat boundary. Calls must run away from the Android main thread. */
interface LlmEngine : AutoCloseable {
    val isLoaded: Boolean

    fun load(request: LlmLoadRequest): LlmLoadResult

    /** Formats messages with the model's embedded chat template when it has one. */
    fun format(messages: List<LlmChatMessage>): String

    /** Returns an exact token count for a formatted prompt. */
    fun countTokens(prompt: String): Int

    fun generate(request: LlmGenerationRequest, onToken: (String) -> Unit): LlmGenerationResult

    fun cancel()

    fun unload()

    override fun close() = unload()
}
