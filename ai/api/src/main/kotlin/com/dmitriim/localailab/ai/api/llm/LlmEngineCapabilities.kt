package com.dmitriim.localailab.ai.api.llm

import com.dmitriim.localailab.core.model.engine.ComputePreference

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

enum class LlmChatTemplateHandling {
    ENGINE_FORMATS_MESSAGES,
    CALLER_PROVIDES_PROMPT,
}

enum class LlmContextManagement {
    EXACT_CALLER_BUDGET,
    ESTIMATED_CALLER_BUDGET,
    RUNTIME_MANAGED,
}

enum class LlmLoadOption {
    CONTEXT_SIZE,
    THREAD_COUNT,
}

enum class LlmGenerationOption {
    MAX_OUTPUT_TOKENS,
    TEMPERATURE,
    TOP_K,
    TOP_P,
    SEED,
}
