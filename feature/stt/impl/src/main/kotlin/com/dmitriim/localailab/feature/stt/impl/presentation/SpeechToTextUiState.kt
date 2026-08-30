package com.dmitriim.localailab.feature.stt.impl.presentation

import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.library.CatalogModel
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.model.manifest.SttRecognitionMode
import com.dmitriim.localailab.core.audio.input.model.AudioLevel
import com.dmitriim.localailab.core.audio.input.model.PcmAudioInput
import com.dmitriim.localailab.core.ui.text.UiText
import com.dmitriim.localailab.feature.models.api.domain.library.BuiltInSpeechToTextModels
import com.dmitriim.localailab.feature.models.api.domain.library.InstalledModel
import com.dmitriim.localailab.feature.models.api.domain.library.ModelValidationState
import com.dmitriim.localailab.feature.stt.api.domain.SpeechTranscriptionMetrics

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
    val errorMessage: UiText? = null,
) {
    val selectedModel: SpeechModelOption? get() = models.firstOrNull { it.id == selectedModelId }
    val availableLanguages: List<SttLanguage>
        get() = selectedModel?.supportedLanguages.orEmpty().ifEmpty { listOf(SttLanguage.ENGLISH) }
}

data class SpeechModelOption(
    val id: ModelId,
    val displayName: String,
    val engineId: EngineId,
    val languages: Set<String>,
    val recognitionMode: SttRecognitionMode,
    val installed: Boolean,
) {
    val supportedLanguages: List<SttLanguage>
        get() = SttLanguage.entries.filter { it.label in languages }
}

enum class SttLanguage(val label: String, val whisperCode: String) {
    ENGLISH("English", "en"),
    RUSSIAN("Russian", "ru"),
    CHINESE("Chinese", "zh"),
    JAPANESE("Japanese", "ja"),
    KOREAN("Korean", "ko"),
    CANTONESE("Cantonese", "yue"),
}

enum class SttOperation { IDLE, RECORDING, STOPPING, IMPORTING, TRANSCRIBING, CANCELLING }

internal fun InstalledModel.toSpeechModelOption(): SpeechModelOption = SpeechModelOption(
    id = manifest.modelId,
    displayName = manifest.displayName,
    engineId = manifest.engineId,
    languages = manifest.languages,
    recognitionMode = manifest.sttRecognitionMode,
    installed = true,
)

internal fun CatalogModel.toSpeechModelOption(): SpeechModelOption = SpeechModelOption(
    id = manifest.modelId,
    displayName = manifest.displayName,
    engineId = manifest.engineId,
    languages = manifest.languages,
    recognitionMode = manifest.sttRecognitionMode,
    installed = false,
)

internal fun InstalledModel.isReadySpeechModel(): Boolean = AiCapability.SPEECH_TO_TEXT in manifest.capabilities &&
    validationState == ModelValidationState.READY

internal fun androidSpeechRecognizerOption() = SpeechModelOption(
    id = BuiltInSpeechToTextModels.ANDROID_SPEECH_RECOGNIZER,
    displayName = "Android On-device SpeechRecognizer",
    engineId = EngineId("android-speech-recognizer"),
    languages = linkedSetOf("English", "Russian", "Chinese", "Japanese", "Korean", "Cantonese"),
    recognitionMode = SttRecognitionMode.STREAMING,
    installed = true,
)
