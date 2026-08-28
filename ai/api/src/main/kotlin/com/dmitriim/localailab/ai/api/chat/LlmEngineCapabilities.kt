package com.dmitriim.localailab.ai.api.chat

import com.dmitriim.localailab.core.model.engine.ComputePreference

/**
 * Immutable declaration of the features supported by one chat runtime.
 *
 * Callers use this declaration to hide unsupported controls and prepare prompts correctly.
 * Runtimes must not advertise an operation they cannot honor. In particular, engine formatting
 * requires [LlmChatFormatter], and token counting requires [LlmTokenCounter].
 */
data class LlmEngineCapabilities(
    val computePreferences: Set<ComputePreference>,
    val streaming: Boolean,
    val cancellation: Boolean,
    val tokenCounting: Boolean,
    val chatTemplateHandling: LlmChatTemplateHandling,
    val systemInstructions: Boolean,
    val contextManagement: LlmContextManagement,
    val loadOptions: Set<LlmLoadOption>,
    val generationOptions: Set<LlmGenerationOption>,
)

/** Identifies who converts [LlmChatMessage] values into the prompt consumed by generation. */
enum class LlmChatTemplateHandling {
    /** The runtime owns the model-specific chat template and implements [LlmChatFormatter]. */
    ENGINE_FORMATS_MESSAGES,

    /** The caller supplies a prompt using its own compatible formatting convention. */
    CALLER_PROVIDES_PROMPT,
}

/** Identifies who makes the context-window fit decision before generation. */
enum class LlmContextManagement {
    /** The caller uses the runtime tokenizer to trim history to an exact budget. */
    EXACT_CALLER_BUDGET,

    /** The caller trims history using an estimate because exact token counting is unavailable. */
    ESTIMATED_CALLER_BUDGET,

    /** The runtime accepts the full prompt and applies any context policy internally. */
    RUNTIME_MANAGED,
}

/** Optional controls accepted while loading a model. */
enum class LlmLoadOption {
    /** Configures the total context-window capacity used by the loaded model. */
    CONTEXT_SIZE,

    /** Configures the CPU worker count; a runtime may interpret zero as an automatic choice. */
    THREAD_COUNT,
}

/** Optional controls accepted for one generation request. */
enum class LlmGenerationOption {
    /** Limits newly generated tokens. */
    MAX_OUTPUT_TOKENS,

    /** Controls sampling randomness. */
    TEMPERATURE,

    /** Limits the candidate set considered while sampling. */
    TOP_K,

    /** Limits the cumulative probability mass considered while sampling. */
    TOP_P,

    /** Requests deterministic sampling when supported by the runtime. */
    SEED,
}
