package com.dmitriim.localailab.ai.api.chat

/** One generation request for a loaded runtime. [prompt] must already use that runtime's format. */
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

/** Complete output and runtime-reported timing/token metrics for one generation. All durations are milliseconds. */
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

/** Why generation stopped. */
enum class LlmFinishReason {
    /** The model emitted its end-of-generation token. */
    STOP_TOKEN,
    /** The requested output-token limit was reached. */
    MAX_TOKENS,
    /** Cancellation was observed; [LlmGenerationResult.text] may contain partial output. */
    CANCELLED,
}
