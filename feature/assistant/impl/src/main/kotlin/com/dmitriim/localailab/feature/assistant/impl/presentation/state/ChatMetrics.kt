package com.dmitriim.localailab.feature.assistant.impl.presentation.state

import com.dmitriim.localailab.ai.api.chat.LlmFinishReason
import com.dmitriim.localailab.ai.api.engine.ComputePreference
import com.dmitriim.localailab.ai.api.profiling.InferenceTelemetry

data class EffectiveChatSettings(
    val computePreference: ComputePreference,
    val systemPrompt: String,
    val temperature: Float,
    val topK: Int,
    val topP: Float,
    val maxOutputTokens: Int,
    val seed: Int?,
    val contextSize: Int,
    val threadCount: Int,
)

data class ChatMetrics(
    val modelName: String,
    val coldStart: Boolean,
    val loadDurationMs: Long,
    val promptTokens: Int?,
    val promptTokensPerSecond: Double?,
    val timeToFirstTokenMs: Long?,
    val generatedTokens: Int?,
    val generatedTokensPerSecond: Double?,
    val totalDurationMs: Long,
    val finishReason: LlmFinishReason,
    val effectiveSettings: EffectiveChatSettings,
    val effectiveThreadCount: Int?,
    val telemetry: InferenceTelemetry? = null,
)
