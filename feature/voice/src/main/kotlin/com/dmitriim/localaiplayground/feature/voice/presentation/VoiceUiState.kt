package com.dmitriim.localaiplayground.feature.voice.presentation

import com.dmitriim.localaiplayground.core.audio.input.model.AudioLevel
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.feature.voice.domain.VoicePipelineMetrics
import java.util.UUID

data class VoiceUiState(
    val speechModels: List<VoiceModelOption> = emptyList(),
    val chatModels: List<VoiceModelOption> = emptyList(),
    val voiceModels: List<VoiceModelOption> = emptyList(),
    val selectedSpeechModelId: ModelId? = null,
    val selectedChatModelId: ModelId? = null,
    val selectedVoiceModelId: ModelId? = null,
    val language: VoiceLanguage = VoiceLanguage.ENGLISH,
    val settings: VoiceSettings = VoiceSettings(),
    val phase: VoicePhase = VoicePhase.IDLE,
    val level: AudioLevel? = null,
    val finalTranscript: String = "",
    val streamingResponse: String = "",
    val conversation: List<VoiceConversationTurn> = emptyList(),
    val contextUsage: VoiceContextUsage? = null,
    val metrics: VoicePipelineMetrics? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
) {
    val selectedSpeechModel get() = speechModels.firstOrNull { it.id == selectedSpeechModelId }
    val selectedChatModel get() = chatModels.firstOrNull { it.id == selectedChatModelId }
    val selectedVoiceModel get() = voiceModels.firstOrNull { it.id == selectedVoiceModelId }

    val estimatedPipelineRamBytes: Long? = listOfNotNull(
        selectedSpeechModel?.approximateRamBytes,
        selectedChatModel?.approximateRamBytes,
        selectedVoiceModel?.approximateRamBytes,
    ).takeIf { it.size == 3 }?.sum()

    val configurationError: String?
        get() = when {
            selectedSpeechModel == null -> "Select an installed speech model."
            selectedChatModel == null -> "Select an installed chat model."
            selectedVoiceModel == null -> "Select an installed voice model."
            !selectedSpeechModel!!.languages.supports(language) ->
                "${selectedSpeechModel!!.displayName} is not configured for ${language.label}."
            !selectedVoiceModel!!.languages.supports(language) ->
                "${selectedVoiceModel!!.displayName} is not configured for ${language.label}."
            else -> null
        }

    val canStart: Boolean get() = phase in setOf(VoicePhase.IDLE, VoicePhase.ERROR) && configurationError == null
}

data class VoiceModelOption(
    val id: ModelId,
    val displayName: String,
    val engineId: EngineId,
    val languages: Set<String>,
    val approximateRamBytes: Long?,
)

enum class VoiceLanguage(val label: String, val code: String) {
    ENGLISH("English", "en"),
    RUSSIAN("Russian", "ru"),
}

private fun Set<String>.supports(language: VoiceLanguage): Boolean =
    language.code in this || language.label in this

enum class VoicePhase(val label: String) {
    IDLE("Ready"),
    LISTENING("Listening"),
    FINALIZING("Finalizing speech"),
    THINKING("Thinking"),
    SPEAKING("Speaking"),
    CANCELLING("Cancelling"),
    ERROR("Needs attention"),
}

data class VoiceSettings(
    val systemPrompt: String = "You are a helpful, concise voice assistant.",
    val temperature: String = "0.7",
    val maxOutputTokens: String = "128",
    val contextSize: String = "512",
    val sttThreadCount: String = "0",
    val llmThreadCount: String = "0",
    val ttsThreadCount: String = "0",
    val speakerId: String = "0",
    val speechRate: String = "1.0",
    val volume: String = "1.0",
)

data class VoiceConversationTurn(
    val id: String = UUID.randomUUID().toString(),
    val userText: String,
    val assistantText: String,
)

data class VoiceContextUsage(
    val promptTokens: Int,
    val contextSize: Int,
    val reservedOutputTokens: Int,
    val omittedTurnCount: Int,
)
