package com.dmitriim.localailab.ai.api.chat

data class LlmGenerationRequest(
    val prompt: String,
    val options: LlmGenerationOptions = LlmGenerationOptions(),
)

/** Typed generation controls. A null value leaves that control to the selected engine. */
data class LlmGenerationOptions(
    val maxTokens: Int? = null,
    val temperature: Float? = null,
    val topK: Int? = null,
    val topP: Float? = null,
    /** Null leaves seed selection to the engine; a value requests that explicit seed. */
    val seed: Int? = null,
) {
    init {
        require(seed == null || seed >= 0) { "An explicit generation seed cannot be negative." }
    }
}

data class LlmGenerationResult(
    val text: String,
    val promptTokenCount: Int?,
    val generatedTokenCount: Int?,
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
