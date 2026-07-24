package com.dmitriim.localaiplayground.ai.api

/**
 * Engine-neutral boundary for the Stage 0 LLM spike.
 *
 * Calls are deliberately suspend-free here: the owner chooses its worker thread and
 * must never invoke an implementation from the Android main thread.
 */
interface LlmEngine : AutoCloseable {
    val isLoaded: Boolean

    fun load(request: LlmLoadRequest): LlmLoadResult

    fun generate(request: LlmGenerationRequest): LlmGenerationResult

    fun cancel()

    fun unload()

    override fun close() = unload()
}

data class LlmLoadRequest(
    val modelPath: String,
    val contextSize: Int = 512,
    val threadCount: Int = 0,
    val requestedBackend: LlmBackend = LlmBackend.CPU,
)

data class LlmGenerationRequest(
    val prompt: String,
    val maxTokens: Int = 32,
)

data class LlmLoadResult(
    val effectiveBackend: LlmBackend,
    val effectiveThreadCount: Int,
    val loadDurationMs: Long,
    val systemInfo: String,
)

data class LlmGenerationResult(
    val text: String,
    val generatedTokenCount: Int,
    val firstTokenLatencyMs: Long?,
    val totalDurationMs: Long,
    val cancelled: Boolean,
)

enum class LlmBackend {
    CPU,
    VULKAN,
    NNAPI,
}
