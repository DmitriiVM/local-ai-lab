package com.dmitriim.localaiplayground.feature.tts.presentation

import com.dmitriim.localaiplayground.core.audio.output.model.GeneratedAudioFile
import com.dmitriim.localaiplayground.core.audio.processing.SpeechAudioEffects
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackState
import com.dmitriim.localaiplayground.core.audio.input.model.AudioLevel
import com.dmitriim.localaiplayground.core.audio.input.storage.ReferenceVoice
import com.dmitriim.localaiplayground.core.model.AiCapability
import com.dmitriim.localaiplayground.core.model.CatalogModel
import com.dmitriim.localaiplayground.core.model.InstalledModel
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelValidationState
import com.dmitriim.localaiplayground.core.model.TtsVoiceDescriptor
import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.core.model.ModelProfileId
import com.dmitriim.localaiplayground.core.model.TtsControl
import com.dmitriim.localaiplayground.core.model.TtsVoiceMode
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
    val audioEffects: SpeechAudioEffects = SpeechAudioEffects(),
    val selectedVoiceId: String? = null,
    val referenceVoices: List<ReferenceVoice> = emptyList(),
    val referenceLevel: AudioLevel? = null,
    val previewVoiceId: String? = null,
    val operation: TtsOperation = TtsOperation.IDLE,
    val playback: SpeechPlaybackState = SpeechPlaybackState(),
    val output: GeneratedAudioFile? = null,
    val metrics: SpeechSynthesisMetrics? = null,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
) {
    val selectedModel: TtsModelOption? get() = models.firstOrNull { it.id == selectedModelId }
    val compatibleVoices: List<TtsVoiceOption>
        get() = selectedModel?.let { model ->
            if (model.voiceMode == TtsVoiceMode.REFERENCE_AUDIO) {
                referenceVoices.map(ReferenceVoice::toTtsVoiceOption)
            } else {
                model.compatibleVoices(language)
            }
        }.orEmpty()
    val selectedVoice: TtsVoiceOption?
        get() = compatibleVoices.firstOrNull { it.id == selectedVoiceId }
    val characterLimit: Int get() = SynthesizeSpeech.MAX_TEXT_CHARACTERS
    val usesReferenceVoice: Boolean
        get() = selectedModel?.voiceMode == TtsVoiceMode.REFERENCE_AUDIO
    val supportsSpeechRate: Boolean
        get() = TtsControl.SPEECH_RATE in selectedModel?.supportedControls.orEmpty()
    val supportsSentenceSilence: Boolean
        get() = TtsControl.SENTENCE_SILENCE in selectedModel?.supportedControls.orEmpty()
}

data class TtsModelOption(
    val id: ModelId,
    val displayName: String,
    val engineId: EngineId,
    val profileType: ModelProfileId,
    val languages: Set<String>,
    val speakerCount: Int?,
    val voiceMode: TtsVoiceMode,
    val supportedControls: Set<TtsControl>,
    val voices: List<TtsVoiceOption>,
)

data class TtsVoiceOption(
    val id: String,
    val displayName: String,
    val speakerId: Int?,
    val languages: Set<String>,
    val description: String?,
    val reference: ReferenceVoice? = null,
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
    PREVIEWING,
    RECORDING_REFERENCE,
    STOPPING_REFERENCE,
    IMPORTING_REFERENCE,
    SYNTHESIZING,
    CANCELLING,
}

internal fun InstalledModel.toTtsModelOption(catalog: List<CatalogModel>): TtsModelOption {
    val catalogManifest = catalog
        .firstOrNull { entry ->
            entry.manifest.modelId == manifest.modelId &&
                entry.manifest.revision == manifest.revision
        }
        ?.manifest
    val metadata = catalogManifest ?: manifest
    val voiceDescriptors = metadata.voices.ifEmpty {
        val count = (metadata.speakerCount ?: manifest.speakerCount ?: 1).coerceAtLeast(1)
        val languages = manifest.languages.mapTo(linkedSetOf(), ::languageCode)
        List(count) { speakerId ->
            TtsVoiceDescriptor(
                id = "speaker-$speakerId",
                displayName = "Speaker ${speakerId + 1}",
                speakerId = speakerId,
                languages = languages,
            )
        }
    }
    return TtsModelOption(
        id = manifest.modelId,
        displayName = manifest.displayName,
        engineId = manifest.engineId,
        profileType = manifest.profileType,
        languages = manifest.languages,
        speakerCount = metadata.speakerCount
            ?: voiceDescriptors.maxOfOrNull { it.speakerId + 1 },
        voiceMode = metadata.ttsVoiceMode,
        supportedControls = metadata.ttsControls,
        voices = voiceDescriptors.map { voice ->
            TtsVoiceOption(
                id = voice.id,
                displayName = voice.displayName,
                speakerId = voice.speakerId,
                languages = voice.languages,
                description = voice.description,
            )
        },
    )
}

private fun ReferenceVoice.toTtsVoiceOption() = TtsVoiceOption(
    id = id,
    displayName = displayName,
    speakerId = null,
    languages = setOf("en"),
    description = "${durationMs / 1_000.0} s · $sourceDescription",
    reference = this,
)

internal fun TtsModelOption.compatibleVoices(language: TtsLanguage): List<TtsVoiceOption> {
    if (languages.none { languageCode(it) == language.code }) return emptyList()
    return voices.filter { voice ->
        voice.languages.isEmpty() || language.code in voice.languages.map(::languageCode)
    }
}

internal fun InstalledModel.isReadyTtsModel(): Boolean =
    AiCapability.TEXT_TO_SPEECH in manifest.capabilities &&
        validationState == ModelValidationState.READY

private const val ENGLISH_SAMPLE = "Local speech synthesis is running entirely on this device."
private const val RUSSIAN_SAMPLE = "Локальный синтез речи полностью выполняется на этом устройстве."
private const val CHINESE_SAMPLE = "本地语音合成完全在这台设备上运行。"

private fun languageCode(value: String): String = when (value.lowercase()) {
    "english" -> "en"
    "russian" -> "ru"
    "chinese" -> "zh"
    "german" -> "de"
    "spanish" -> "es"
    "french" -> "fr"
    "hindi" -> "hi"
    "italian" -> "it"
    "japanese" -> "ja"
    "portuguese" -> "pt"
    else -> value.lowercase()
}
