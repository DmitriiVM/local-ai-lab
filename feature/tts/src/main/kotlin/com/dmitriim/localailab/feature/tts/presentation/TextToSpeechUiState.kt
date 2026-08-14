package com.dmitriim.localailab.feature.tts.presentation

import com.dmitriim.localailab.ai.api.system.SystemTextToSpeechVoice
import com.dmitriim.localailab.core.audio.input.model.AudioLevel
import com.dmitriim.localailab.core.audio.input.storage.ReferenceVoice
import com.dmitriim.localailab.core.audio.output.model.GeneratedAudioFile
import com.dmitriim.localailab.core.audio.output.model.SpeechPlaybackState
import com.dmitriim.localailab.core.audio.processing.SpeechAudioEffects
import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.library.BuiltInTextToSpeechModels
import com.dmitriim.localailab.core.model.library.CatalogModel
import com.dmitriim.localailab.core.model.library.InstalledModel
import com.dmitriim.localailab.core.model.library.ModelValidationState
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.manifest.ModelProfileId
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.manifest.TtsControl
import com.dmitriim.localailab.core.model.manifest.TtsVoiceDescriptor
import com.dmitriim.localailab.core.model.manifest.TtsVoiceMode
import com.dmitriim.localailab.core.ui.text.UiText
import com.dmitriim.localailab.core.voice.tts.SpeechSynthesisMetrics
import com.dmitriim.localailab.core.voice.tts.SynthesizeSpeech

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
    val errorMessage: UiText? = null,
    val statusMessage: UiText? = null,
) {
    val selectedModel: TtsModelOption? get() = models.firstOrNull { it.id == selectedModelId }
    val compatibleVoices: List<TtsVoiceOption>
        get() = TtsVoiceResolver.forModel(selectedModel, language, referenceVoices)
    val selectedVoice: TtsVoiceOption?
        get() = compatibleVoices.firstOrNull { it.id == selectedVoiceId }
    val characterLimit: Int get() = SynthesizeSpeech.MAX_TEXT_CHARACTERS
    val usesReferenceVoice: Boolean
        get() = selectedModel?.voiceMode == TtsVoiceMode.REFERENCE_AUDIO
    val supportsSpeechRate: Boolean
        get() = TtsControl.SPEECH_RATE in selectedModel?.supportedControls.orEmpty()
    val supportsSentenceSilence: Boolean
        get() = TtsControl.SENTENCE_SILENCE in selectedModel?.supportedControls.orEmpty()
    val usesPlatformVoice: Boolean
        get() = selectedModel?.voiceMode == TtsVoiceMode.PLATFORM
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
    val installed: Boolean,
)

data class TtsVoiceOption(
    val id: String,
    val displayName: String,
    val speakerId: Int?,
    val languages: Set<String>,
    val description: String?,
    val reference: ReferenceVoice? = null,
    val platformVoiceId: String? = null,
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
    return manifest.toTtsModelOption(metadata, installed = true)
}

internal fun CatalogModel.toTtsModelOption(): TtsModelOption = manifest.toTtsModelOption(manifest, installed = false)

private fun ModelManifest.toTtsModelOption(
    metadata: ModelManifest,
    installed: Boolean,
): TtsModelOption {
    val voiceDescriptors = metadata.voices.ifEmpty {
        val count = (metadata.speakerCount ?: this.speakerCount ?: 1).coerceAtLeast(1)
        val languages = this.languages.mapTo(linkedSetOf(), ::languageCode)
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
        id = modelId,
        displayName = displayName,
        engineId = engineId,
        profileType = profileType,
        languages = languages,
        speakerCount = metadata.speakerCount ?: voiceDescriptors.maxOfOrNull { it.speakerId + 1 },
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
        installed = installed,
    )
}

internal fun TtsModelOption.compatibleVoices(language: TtsLanguage): List<TtsVoiceOption> {
    if (languages.none { languageCode(it) == language.code }) return emptyList()
    return voices.filter { voice ->
        voice.languages.isEmpty() || language.code in voice.languages.map(::languageCode)
    }
}

internal fun InstalledModel.isReadyTtsModel(): Boolean = AiCapability.TEXT_TO_SPEECH in manifest.capabilities &&
    validationState == ModelValidationState.READY

internal fun androidTextToSpeechOption(
    systemVoices: List<SystemTextToSpeechVoice>,
): TtsModelOption {
    val voices = systemVoices.map { voice ->
        TtsVoiceOption(
            id = voice.id,
            displayName = voice.displayName,
            speakerId = null,
            languages = setOf(voice.languageTag),
            description = voice.description,
            platformVoiceId = voice.id,
        )
    }
    return TtsModelOption(
        id = BuiltInTextToSpeechModels.ANDROID_TEXT_TO_SPEECH,
        displayName = "Android On-device TextToSpeech",
        engineId = EngineId("android-text-to-speech"),
        profileType = ModelProfileIds.ANDROID_TEXT_TO_SPEECH_TTS,
        languages = TtsLanguage.entries
            .filter { language ->
                systemVoices.any { voice -> languageCode(voice.languageTag) == language.code }
            }
            .mapTo(linkedSetOf(), TtsLanguage::label),
        speakerCount = null,
        voiceMode = TtsVoiceMode.PLATFORM,
        supportedControls = setOf(
            TtsControl.LANGUAGE,
            TtsControl.SPEAKER,
            TtsControl.SPEECH_RATE,
        ),
        voices = voices,
        installed = true,
    )
}

internal fun ttsModelOptions(
    installedModels: List<InstalledModel>,
    catalogModels: List<CatalogModel>,
): List<TtsModelOption> {
    val installedById = installedModels.filter { it.isReadyTtsModel() }.associateBy { it.manifest.modelId }
    val catalogTtsModels = catalogModels.filter { AiCapability.TEXT_TO_SPEECH in it.manifest.capabilities }
    return buildList {
        catalogTtsModels.forEach { entry ->
            add(installedById[entry.manifest.modelId]?.toTtsModelOption(catalogModels) ?: entry.toTtsModelOption())
        }
        installedById
            .filterKeys { id -> catalogTtsModels.none { it.manifest.modelId == id } }
            .values
            .mapTo(this) { it.toTtsModelOption(catalogModels) }
    }
}

private const val ENGLISH_SAMPLE = "Local speech synthesis is running entirely on this device."
private const val RUSSIAN_SAMPLE = "Локальный синтез речи полностью выполняется на этом устройстве."
private const val CHINESE_SAMPLE = "本地语音合成完全在这台设备上运行。"

private fun languageCode(value: String): String {
    val normalized = value.lowercase()
    return when (normalized) {
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
        else -> normalized.substringBefore('-').substringBefore('_')
    }
}
