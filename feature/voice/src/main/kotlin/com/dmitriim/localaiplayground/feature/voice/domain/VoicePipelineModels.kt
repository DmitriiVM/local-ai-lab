package com.dmitriim.localaiplayground.feature.voice.domain

import java.util.UUID

enum class VoiceTurnPhase { LISTENING, FINALIZING, THINKING, SPEAKING }

data class VoicePipelineInfo(val speechModel: String, val chatModel: String, val voiceModel: String)

data class VoiceContextUsage(
    val promptTokens: Int,
    val contextSize: Int,
    val reservedOutputTokens: Int,
    val omittedTurnCount: Int,
)

data class VoiceComponentRunIds(val stt: String, val llm: String, val tts: String) {
    companion object {
        fun newIds() = VoiceComponentRunIds(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString())
    }
}

data class VoicePipelineMetrics(
    val listeningDurationMs: Long,
    val speechFinalizationDurationMs: Long,
    val sttProcessingDurationMs: Long,
    val llmTimeToFirstTokenMs: Long?,
    val llmCompletionDurationMs: Long,
    val ttsTimeToFirstChunkMs: Long?,
    val ttsTimeToFirstWriteMs: Long?,
    val ttsTimeToFirstPresentationMs: Long?,
    val ttsCompletionDurationMs: Long,
    val endToEndTimeToFirstOutputMs: Long?,
    val sttModelName: String,
    val chatModelName: String,
    val voiceModelName: String,
    val componentRunIds: VoiceComponentRunIds,
)
