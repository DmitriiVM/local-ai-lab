package com.dmitriim.localailab.ai.api.llm

/** Engine-neutral local chat boundary. Calls must run away from the Android main thread. */
interface LlmEngine {
    val isLoaded: Boolean

    fun load(request: LlmLoadRequest): LlmLoadResult

    fun generate(request: LlmGenerationRequest, onToken: (String) -> Unit): LlmGenerationResult

    fun cancel()

    fun unload()

}
