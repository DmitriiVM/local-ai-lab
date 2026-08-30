package com.dmitriim.localailab.feature.assistant.impl.presentation

import android.app.Application
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.chat.LlmEngineCapabilities
import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.library.CatalogModel
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.ai.api.model.manifest.TtsControl
import com.dmitriim.localailab.ai.api.model.manifest.TtsVoiceDescriptor
import com.dmitriim.localailab.ai.api.model.manifest.TtsVoiceMode
import com.dmitriim.localailab.ai.api.system.SystemModelProfileKeys
import com.dmitriim.localailab.ai.api.system.SystemTextToSpeechVoice
import com.dmitriim.localailab.core.audio.input.storage.ReferenceVoice
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.ChatModelOption
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.SpeechModelOption
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.TtsModelOption
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.TtsVoiceOption
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.normalizeLanguageCode
import com.dmitriim.localailab.feature.models.api.domain.library.BuiltInSpeechToTextModels
import com.dmitriim.localailab.feature.models.api.domain.library.BuiltInTextToSpeechModels
import com.dmitriim.localailab.feature.models.api.domain.library.InstalledModel
import com.dmitriim.localailab.feature.models.api.domain.library.ModelValidationState

internal fun chatModelOptions(
    installedModels: List<InstalledModel>,
    catalogModels: List<CatalogModel>,
    capabilitiesFor: (EngineId) -> LlmEngineCapabilities?,
): List<ChatModelOption> = capabilityModels(installedModels, catalogModels, AiCapability.CHAT) { manifest, installed ->
    ChatModelOption(
        id = manifest.modelId,
        displayName = manifest.displayName,
        engineId = manifest.engineId,
        capabilities = capabilitiesFor(manifest.engineId),
        defaultContextSize = manifest.contextSize ?: 512,
        installed = installed,
    )
}

internal fun speechModelOptions(
    application: Application,
    installedModels: List<InstalledModel>,
    catalogModels: List<CatalogModel>,
    includeAndroidRecognizer: Boolean,
): List<SpeechModelOption> = buildList {
    if (includeAndroidRecognizer) {
        add(
            SpeechModelOption(
                id = BuiltInSpeechToTextModels.ANDROID_SPEECH_RECOGNIZER,
                displayName = application.getString(CoreUiR.string.assistant_model_android_speech_recognizer),
                engineId = EngineId("android-speech-recognizer"),
                languages = setOf("en", "ru", "zh", "ja", "ko", "yue"),
                sampleRateHz = 16_000,
                installed = true,
            ),
        )
    }
    addAll(
        capabilityModels(installedModels, catalogModels, AiCapability.SPEECH_TO_TEXT) { manifest, installed ->
            SpeechModelOption(
                id = manifest.modelId,
                displayName = manifest.displayName,
                engineId = manifest.engineId,
                languages = manifest.languages,
                sampleRateHz = manifest.sampleRateHz ?: 16_000,
                installed = installed,
            )
        },
    )
}

internal fun textToSpeechModelOptions(
    application: Application,
    installedModels: List<InstalledModel>,
    catalogModels: List<CatalogModel>,
    referenceVoices: List<ReferenceVoice>,
    systemVoices: List<SystemTextToSpeechVoice>,
): List<TtsModelOption> = buildList {
    if (systemVoices.isNotEmpty()) add(androidTextToSpeechOption(application, systemVoices))
    addAll(
        capabilityModels(installedModels, catalogModels, AiCapability.TEXT_TO_SPEECH) { manifest, installed ->
            val catalogManifest = catalogModels.firstOrNull {
                it.manifest.modelId == manifest.modelId && it.manifest.revision == manifest.revision
            }?.manifest ?: manifest
            catalogManifest.toTtsOption(application, installed, referenceVoices)
        },
    )
}

private inline fun <T> capabilityModels(
    installedModels: List<InstalledModel>,
    catalogModels: List<CatalogModel>,
    capability: AiCapability,
    transform: (ModelManifest, Boolean) -> T,
): List<T> {
    val ready = installedModels.filter {
        capability in it.manifest.capabilities && it.validationState == ModelValidationState.READY
    }
    val installedById = ready.associateBy { it.manifest.modelId }
    val catalog = catalogModels.filter { capability in it.manifest.capabilities }
    val catalogIds = catalog.mapTo(mutableSetOf()) { it.manifest.modelId }
    return buildList {
        catalog.forEach { entry ->
            val installed = installedById[entry.manifest.modelId]
            add(transform(installed?.manifest ?: entry.manifest, installed != null))
        }
        ready.filterNot { it.manifest.modelId in catalogIds }.forEach { add(transform(it.manifest, true)) }
    }
}

private fun ModelManifest.toTtsOption(
    application: Application,
    installed: Boolean,
    referenceVoices: List<ReferenceVoice>,
): TtsModelOption {
    val voiceDescriptors = voices.ifEmpty {
        val count = (speakerCount ?: 1).coerceAtLeast(1)
        List(count) { speakerId ->
            TtsVoiceDescriptor(
                id = "speaker-$speakerId",
                displayName = application.getString(CoreUiR.string.assistant_model_speaker, speakerId + 1),
                speakerId = speakerId,
                languages = languages.mapTo(linkedSetOf(), ::normalizeLanguageCode),
            )
        }
    }
    val options = if (ttsVoiceMode == TtsVoiceMode.REFERENCE_AUDIO) {
        referenceVoices.map { reference ->
            TtsVoiceOption(
                id = reference.id,
                displayName = reference.displayName,
                speakerId = null,
                languages = setOf("en"),
                description = application.getString(
                    CoreUiR.string.assistant_reference_voice_duration,
                    reference.durationMs / 1_000.0,
                    reference.sourceDescription,
                ),
                reference = reference,
            )
        }
    } else {
        voiceDescriptors.map { voice ->
            TtsVoiceOption(
                id = voice.id,
                displayName = voice.displayName,
                speakerId = voice.speakerId,
                languages = voice.languages,
                description = voice.description,
            )
        }
    }
    return TtsModelOption(
        id = modelId,
        displayName = displayName,
        engineId = engineId,
        profileType = profileType,
        languages = languages,
        speakerCount = speakerCount ?: voiceDescriptors.maxOfOrNull { it.speakerId + 1 },
        voiceMode = ttsVoiceMode,
        supportedControls = ttsControls,
        voices = options,
        installed = installed,
    )
}

private fun androidTextToSpeechOption(
    application: Application,
    voices: List<SystemTextToSpeechVoice>,
): TtsModelOption = TtsModelOption(
    id = BuiltInTextToSpeechModels.ANDROID_TEXT_TO_SPEECH,
    displayName = application.getString(CoreUiR.string.assistant_model_android_text_to_speech),
    engineId = SystemModelProfileKeys.ANDROID_TEXT_TO_SPEECH.engineId,
    profileType = SystemModelProfileKeys.ANDROID_TEXT_TO_SPEECH.profileId,
    languages = voices.mapTo(linkedSetOf()) { normalizeLanguageCode(it.languageTag) },
    speakerCount = null,
    voiceMode = TtsVoiceMode.PLATFORM,
    supportedControls = setOf(TtsControl.LANGUAGE, TtsControl.SPEAKER, TtsControl.SPEECH_RATE),
    voices = voices.map { voice ->
        TtsVoiceOption(
            id = voice.id,
            displayName = voice.displayName,
            speakerId = null,
            languages = setOf(voice.languageTag),
            description = voice.description,
            platformVoiceId = voice.id,
        )
    },
    installed = true,
)
