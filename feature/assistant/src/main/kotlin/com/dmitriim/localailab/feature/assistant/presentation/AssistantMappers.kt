package com.dmitriim.localailab.feature.assistant.presentation

import com.dmitriim.localailab.feature.assistant.domain.ChatContextUsage
import com.dmitriim.localailab.feature.assistant.domain.ChatGenerationConfig
import com.dmitriim.localailab.feature.assistant.domain.ChatTurn
import com.dmitriim.localailab.feature.assistant.domain.ChatTurnRole

internal fun EffectiveChatSettings.toDomain() = ChatGenerationConfig(
    computePreference = computePreference,
    systemPrompt = systemPrompt,
    temperature = temperature,
    topK = topK,
    topP = topP,
    maxOutputTokens = maxOutputTokens,
    seed = seed,
    contextSize = contextSize,
    threadCount = threadCount,
)

internal fun ChatMessage.toDomain() = ChatTurn(
    role = when (role) {
        ChatMessageRole.USER -> ChatTurnRole.USER
        ChatMessageRole.ASSISTANT -> ChatTurnRole.ASSISTANT
    },
    content = content,
)

internal fun ChatContextUsage.toUi() = ContextUsage(
    promptTokens = promptTokens,
    promptTokensEstimated = promptTokensEstimated,
    contextSize = contextSize,
    reservedOutputTokens = reservedOutputTokens,
    omittedMessageCount = omittedTurnCount,
    contextManagement = contextManagement,
)
