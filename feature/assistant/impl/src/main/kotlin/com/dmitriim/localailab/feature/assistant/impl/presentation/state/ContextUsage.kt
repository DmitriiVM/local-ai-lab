package com.dmitriim.localailab.feature.assistant.impl.presentation.state

import com.dmitriim.localailab.ai.api.chat.LlmContextManagement

data class ContextUsage(
    val promptTokens: Int?,
    val promptTokensEstimated: Boolean,
    val contextSize: Int?,
    val reservedOutputTokens: Int?,
    val omittedMessageCount: Int,
    val contextManagement: LlmContextManagement,
)
