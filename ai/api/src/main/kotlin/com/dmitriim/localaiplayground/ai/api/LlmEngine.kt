package com.dmitriim.localaiplayground.ai.api

import com.dmitriim.localaiplayground.core.model.ModelProfileId

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

data class LlmLoadRequest(
    val profileType: ModelProfileId,
    val modelPath: String,
    val contextSize: Int = 512,
    val threadCount: Int = 0,
    val requestedBackend: LlmBackend = LlmBackend.CPU,
)

data class LlmLoadResult(
    val effectiveBackend: LlmBackend,
    val effectiveThreadCount: Int,
    val loadDurationMs: Long,
    val systemInfo: String,
    val coldStart: Boolean,
)

data class LlmChatMessage(
    val role: LlmChatRole,
    val content: String,
)

enum class LlmChatRole(val wireName: String) {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
}

data class LlmGenerationRequest(
    val prompt: String,
    val maxTokens: Int = 128,
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.9f,
    /** -1 delegates seed selection to llama.cpp. */
    val seed: Int = -1,
)

data class LlmGenerationResult(
    val text: String,
    val promptTokenCount: Int,
    val generatedTokenCount: Int,
    val firstTokenLatencyMs: Long?,
    val promptDurationMs: Long,
    val generationDurationMs: Long,
    val totalDurationMs: Long,
    val finishReason: LlmFinishReason,
)

enum class LlmFinishReason {
    STOP_TOKEN,
    MAX_TOKENS,
    CANCELLED,
}

enum class LlmBackend {
    CPU,
    VULKAN,
    NNAPI,
}
