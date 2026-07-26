package com.dmitriim.localaiplayground.feature.tts.presentation

import com.dmitriim.localaiplayground.core.audio.output.model.GeneratedAudioFile
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackState
import com.dmitriim.localaiplayground.core.model.AiCapability
import com.dmitriim.localaiplayground.core.model.InstalledModel
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelValidationState
import com.dmitriim.localaiplayground.feature.tts.domain.SpeechSynthesisMetrics
import com.dmitriim.localaiplayground.feature.tts.domain.SynthesizeSpeech

data class TextToSpeechUiState(
    val models: List<TtsModelOption> = emptyList(),
    val selectedModelId: ModelId? = null,
    val text: String = ENGLISH_SAMPLE,
    val language: TtsLanguage = TtsLanguage.ENGLISH,
    val speed: Float = 1f,
    val sentenceSilenceScale: Float = 1f,
    val volume: Float = 1f,
    val threadCount: String = "0",
    val speakerId: Int = 0,
    val speakerCount: Int = 1,
    val operation: TtsOperation = TtsOperation.IDLE,
    val playback: SpeechPlaybackState = SpeechPlaybackState(),
    val output: GeneratedAudioFile? = null,
    val metrics: SpeechSynthesisMetrics? = null,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
) {
    val selectedModel: TtsModelOption? get() = models.firstOrNull { it.id == selectedModelId }
    val characterLimit: Int get() = SynthesizeSpeech.MAX_TEXT_CHARACTERS
}

data class TtsModelOption(
    val id: ModelId,
    val displayName: String,
    val languages: Set<String>,
)

enum class TtsLanguage(
    val label: String,
    val code: String,
    val sample: String,
) {
    ENGLISH("English", "en", ENGLISH_SAMPLE),
    RUSSIAN("Russian", "ru", RUSSIAN_SAMPLE),
    CHINESE("Chinese", "zh", CHINESE_SAMPLE),
}

enum class TtsOperation {
    IDLE,
    SYNTHESIZING,
    CANCELLING,
}

internal fun InstalledModel.toTtsModelOption(): TtsModelOption = TtsModelOption(
    id = manifest.modelId,
    displayName = manifest.displayName,
    languages = manifest.languages,
)

internal fun InstalledModel.isReadyTtsModel(): Boolean =
    AiCapability.TEXT_TO_SPEECH in manifest.capabilities &&
        validationState == ModelValidationState.READY

private const val ENGLISH_SAMPLE = "Local speech synthesis is running entirely on this device."
private const val RUSSIAN_SAMPLE = "Локальный синтез речи полностью выполняется на этом устройстве."
private const val CHINESE_SAMPLE = "本地语音合成完全在这台设备上运行。"
