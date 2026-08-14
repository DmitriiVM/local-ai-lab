package com.dmitriim.localailab.feature.assistant.domain

import com.dmitriim.localailab.ai.api.llm.LlmGenerationResult
import com.dmitriim.localailab.ai.api.llm.LlmLoadResult
import com.dmitriim.localailab.core.performance.InferenceTelemetry

internal sealed interface ChatGenerationEvent {
    data class Prepared(val contextUsage: ChatContextUsage) : ChatGenerationEvent
    data class Token(val text: String) : ChatGenerationEvent
    data class Completed(
        val modelName: String,
        val load: LlmLoadResult,
        val generation: LlmGenerationResult,
        val telemetry: InferenceTelemetry,
    ) : ChatGenerationEvent
}
