package com.dmitriim.localaiplayground.feature.assistant.domain

import com.dmitriim.localaiplayground.ai.api.llm.LlmContextManagement
import com.dmitriim.localaiplayground.core.model.engine.ComputePreference
import com.dmitriim.localaiplayground.core.model.manifest.ModelId

internal data class ChatGenerationRequest(
    val modelId: ModelId,
    val turns: List<ChatTurn>,
    val config: ChatGenerationConfig,
)

internal data class ChatTurn(
    val role: ChatTurnRole,
    val content: String,
)

internal enum class ChatTurnRole { USER, ASSISTANT }

internal data class ChatGenerationConfig(
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

internal data class ChatContextUsage(
    val promptTokens: Int?,
    val promptTokensEstimated: Boolean,
    val contextSize: Int?,
    val reservedOutputTokens: Int?,
    val omittedTurnCount: Int,
    val contextManagement: LlmContextManagement,
)
