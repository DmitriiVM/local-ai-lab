package com.dmitriim.localaiplayground.feature.voice.domain

import com.dmitriim.localaiplayground.core.model.runtime.ChatModelReference
import com.dmitriim.localaiplayground.core.model.runtime.SpeechToTextModelReference
import com.dmitriim.localaiplayground.core.model.runtime.TextToSpeechModelReference

internal data class PreparedVoicePipeline(
    val speech: SpeechToTextModelReference,
    val chat: ChatModelReference,
    val voice: TextToSpeechModelReference,
) {
    fun toInfo() = VoicePipelineInfo(speech.displayName, chat.displayName, voice.displayName)
}

internal data class TranscribedVoice(val text: String, val processingDurationMs: Long)

internal data class GeneratedVoiceResponse(
    val text: String,
    val llmTimeToFirstTokenMs: Long?,
    val llmCompletionDurationMs: Long,
)

internal data class VoiceSpeechMetrics(
    val timeToFirstChunkMs: Long?,
    val timeToFirstWriteMs: Long?,
    val timeToFirstPresentationMs: Long?,
    val completionDurationMs: Long,
)
