package com.dmitriim.localailab.ai.api.chat

/** Shared local-chat execution lifecycle. Calls must run away from the Android main thread. */
interface ChatExecution {
    val isLoaded: Boolean

    fun load(request: LlmLoadRequest): LlmLoadResult

    fun generate(request: LlmGenerationRequest, onToken: (String) -> Unit): LlmGenerationResult

    fun cancel()

    fun unload()
}
