package com.dmitriim.localaiplayground.feature.chat.presentation

import com.dmitriim.localaiplayground.feature.chat.domain.ChatContextUsage
import com.dmitriim.localaiplayground.feature.chat.domain.ChatGenerationConfig
import com.dmitriim.localaiplayground.feature.chat.domain.ChatTurn
import com.dmitriim.localaiplayground.feature.chat.domain.ChatTurnRole

internal fun EffectiveChatSettings.toDomain() = ChatGenerationConfig(
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
    contextSize = contextSize,
    reservedOutputTokens = reservedOutputTokens,
    omittedMessageCount = omittedTurnCount,
)
