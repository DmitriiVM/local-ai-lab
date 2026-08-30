package com.dmitriim.localailab.feature.assistant.impl.presentation

import com.dmitriim.localailab.feature.assistant.impl.domain.ChatContextUsage
import com.dmitriim.localailab.feature.assistant.impl.domain.ChatGenerationConfig
import com.dmitriim.localailab.feature.assistant.impl.domain.ChatTurn
import com.dmitriim.localailab.feature.assistant.impl.domain.ChatTurnRole
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.ChatMessage
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.ChatMessageRole
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.ContextUsage
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.EffectiveChatSettings

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
