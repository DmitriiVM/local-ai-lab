package com.dmitriim.localaiplayground.feature.stt.presentation

import com.dmitriim.localaiplayground.core.audio.AudioLevel
import com.dmitriim.localaiplayground.core.audio.PcmAudioInput
import com.dmitriim.localaiplayground.core.model.InstalledModel
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelValidationState
import com.dmitriim.localaiplayground.core.model.AiCapability
import com.dmitriim.localaiplayground.core.model.RuntimeProfileType
import com.dmitriim.localaiplayground.feature.stt.domain.SpeechTranscriptionMetrics

data class SpeechToTextUiState(
    val models: List<SpeechModelOption> = emptyList(),
    val selectedModelId: ModelId? = null,
    val language: SttLanguage = SttLanguage.ENGLISH,
    val threadCount: String = "0",
    val operation: SttOperation = SttOperation.IDLE,
    val input: PcmAudioInput? = null,
    val level: AudioLevel? = null,
    val transcript: String = "",
    val metrics: SpeechTranscriptionMetrics? = null,
    val errorMessage: String? = null,
) {
    val selectedModel: SpeechModelOption? get() = models.firstOrNull { it.id == selectedModelId }
}

data class SpeechModelOption(val id: ModelId, val displayName: String, val languages: Set<String>)

enum class SttLanguage(val label: String, val whisperCode: String) {
    ENGLISH("English", "en"),
    RUSSIAN("Russian", "ru"),
}

enum class SttOperation { IDLE, RECORDING, STOPPING, IMPORTING, TRANSCRIBING, CANCELLING }

internal fun InstalledModel.toSpeechModelOption(): SpeechModelOption = SpeechModelOption(
    id = manifest.modelId,
    displayName = manifest.displayName,
    languages = manifest.languages,
)

internal fun InstalledModel.isReadySpeechModel(): Boolean =
    AiCapability.SPEECH_TO_TEXT in manifest.capabilities &&
        validationState == ModelValidationState.READY &&
        manifest.profileType == RuntimeProfileType.WHISPER_STT
