package com.dmitriim.localaiplayground.feature.voice.domain

import com.dmitriim.localaiplayground.core.audio.input.model.AudioLevel
import com.dmitriim.localaiplayground.core.model.ChatModelReference
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.SpeechToTextModelReference
import com.dmitriim.localaiplayground.core.model.TextToSpeechModelReference
import com.dmitriim.localaiplayground.feature.voice.presentation.VoiceConversationTurn
import java.util.UUID

data class VoiceTurnRequest(
    val speechModelId: ModelId,
    val chatModelId: ModelId,
    val voiceModelId: ModelId,
    val languageCode: String,
    val systemPrompt: String,
    val temperature: Float,
    val maxOutputTokens: Int,
    val contextSize: Int,
    val sttThreadCount: Int,
    val llmThreadCount: Int,
    val ttsThreadCount: Int,
    val speakerId: Int,
    val speechRate: Float,
    val volume: Float,
    val history: List<VoiceConversationTurn>,
) {
    fun validate() {
        require(languageCode in setOf("en", "ru")) { "Select a supported conversation language." }
        require(temperature in 0f..2f) { "Temperature must be between 0 and 2." }
        require(contextSize in 128..32_768) { "Context size must be between 128 and 32,768 tokens." }
        require(maxOutputTokens in 1 until contextSize) { "Maximum output must be positive and smaller than context size." }
        require(sttThreadCount in 0..64 && llmThreadCount in 0..64 && ttsThreadCount in 0..64) {
            "Thread counts must be between 0 and 64."
        }
        require(speakerId >= 0) { "Speaker ID cannot be negative." }
        require(speechRate in 0.5f..2f) { "Speech rate must be between 0.5 and 2.0." }
        require(volume in 0f..1f) { "Playback volume must be between 0 and 1." }
    }
}

sealed interface VoicePipelineEvent {
    data class Prepared(val pipeline: VoicePipelineInfo, val componentIds: VoiceComponentRunIds) : VoicePipelineEvent
    data class Phase(val value: VoiceTurnPhase) : VoicePipelineEvent
    data class Level(val value: AudioLevel) : VoicePipelineEvent
    data class FinalTranscript(val value: String) : VoicePipelineEvent
    data class ContextPrepared(val value: VoiceContextUsage) : VoicePipelineEvent
    data class AssistantToken(val value: String) : VoicePipelineEvent
    data class AssistantCompleted(val value: String) : VoicePipelineEvent
    data class Completed(val metrics: VoicePipelineMetrics) : VoicePipelineEvent
}

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
