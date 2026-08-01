package com.dmitriim.localaiplayground.feature.voice.domain

import com.dmitriim.localaiplayground.core.model.runs.RunModelSnapshot

data class CompletedVoiceTurn(
    val conversationId: String,
    val startedAtEpochMs: Long,
    val request: VoiceTurnRequest,
    val transcript: String,
    val response: String,
    val conversation: List<VoiceConversationSnapshot>,
    val metrics: VoicePipelineMetrics,
    val speechModel: RunModelSnapshot?,
    val chatModel: RunModelSnapshot?,
    val voiceModel: RunModelSnapshot?,
)

data class VoiceConversationSnapshot(
    val id: String,
    val userText: String,
    val assistantText: String,
)
