package com.dmitriim.localaiplayground.feature.chat.domain

import com.dmitriim.localaiplayground.ai.api.llm.LlmGenerationResult
import com.dmitriim.localaiplayground.ai.api.llm.LlmLoadResult

internal sealed interface ChatGenerationEvent {
    data class Prepared(val contextUsage: ChatContextUsage) : ChatGenerationEvent
    data class Token(val text: String) : ChatGenerationEvent
    data class Completed(
        val modelName: String,
        val load: LlmLoadResult,
        val generation: LlmGenerationResult,
    ) : ChatGenerationEvent
}
