package com.dmitriim.localaiplayground.ai.api.llm

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
