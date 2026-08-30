package com.dmitriim.localailab.feature.assistant.impl.domain.chat

import com.dmitriim.localailab.ai.api.chat.LlmGenerationResult
import com.dmitriim.localailab.ai.api.chat.LlmLoadResult
import com.dmitriim.localailab.ai.api.profiling.InferenceTelemetry

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
