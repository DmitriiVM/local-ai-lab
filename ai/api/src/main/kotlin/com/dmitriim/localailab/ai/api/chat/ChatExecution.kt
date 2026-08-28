package com.dmitriim.localailab.ai.api.chat

/**
 * Lifecycle for one local chat runtime.
 *
 * All calls must run away from the Android main thread. Call [load] successfully before
 * [generate]. Calls are serialized by concrete runtimes; callers must not assume that concurrent
 * generation is supported. [cancel] is best-effort and [unload] releases model resources.
 */
interface ChatExecution {
    /** Whether a model is currently loaded and ready to format or generate. */
    val isLoaded: Boolean

    /** Loads or reuses the model and returns the effective execution configuration. */
    fun load(request: LlmLoadRequest): LlmLoadResult

    /**
     * Generates one response from a runtime-formatted prompt.
     *
     * [onToken] receives ordered, incremental text chunks on the runtime's execution thread.
     * Implementations must return the complete generated text, including every delivered chunk.
     */
    fun generate(request: LlmGenerationRequest, onToken: (String) -> Unit): LlmGenerationResult

    /** Requests cancellation of the active generation without unloading the model. */
    fun cancel()

    /** Cancels active work if necessary and releases all resources owned by the loaded model. */
    fun unload()
}
