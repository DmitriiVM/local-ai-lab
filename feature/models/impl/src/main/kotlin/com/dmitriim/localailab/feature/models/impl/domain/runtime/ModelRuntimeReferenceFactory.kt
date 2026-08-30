package com.dmitriim.localailab.feature.models.impl.domain.runtime

import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.ai.api.model.manifest.SttRecognitionMode
import com.dmitriim.localailab.ai.api.model.manifest.TtsControl
import com.dmitriim.localailab.ai.api.model.manifest.TtsVoiceMode
import com.dmitriim.localailab.ai.api.model.runtime.ChatModelReference
import com.dmitriim.localailab.ai.api.model.runtime.ModelArtifactReference
import com.dmitriim.localailab.ai.api.system.SystemModelProfileKeys
import com.dmitriim.localailab.feature.models.api.domain.library.BuiltInSpeechToTextModels
import com.dmitriim.localailab.feature.models.api.domain.library.BuiltInTextToSpeechModels
import com.dmitriim.localailab.feature.models.api.domain.runtime.SpeechToTextModelReference
import com.dmitriim.localailab.feature.models.api.domain.runtime.TextToSpeechModelReference

/** Builds runtime references from a validated model manifest and its resolved artifacts. */
internal object ModelRuntimeReferenceFactory {
    fun chat(
        manifest: ModelManifest,
        artifacts: List<ModelArtifactReference>,
    ): ChatModelReference {
        require(AiCapability.CHAT in manifest.capabilities) {
            "This installed model is not a compatible local chat model."
        }
        require(manifest.files.any { it.role == ModelFileRoles.PRIMARY_MODEL }) {
            "The chat model does not declare a primary model artifact."
        }
        return ChatModelReference.ArtifactBacked(
            modelId = manifest.modelId,
            displayName = manifest.displayName,
            engineId = manifest.engineId,
            profileType = manifest.profileType,
            defaultContextSize = manifest.contextSize ?: 512,
            artifacts = artifacts,
        )
    }

    fun speechToText(
        manifest: ModelManifest,
        modelDirectory: String,
        artifacts: List<ModelArtifactReference>,
    ): SpeechToTextModelReference {
        require(AiCapability.SPEECH_TO_TEXT in manifest.capabilities) {
            "This installed model is not a compatible local speech-to-text model."
        }
        return SpeechToTextModelReference(
            modelId = manifest.modelId,
            displayName = manifest.displayName,
            engineId = manifest.engineId,
            profileType = manifest.profileType,
            modelDirectory = modelDirectory,
            artifacts = artifacts,
            sampleRateHz = manifest.sampleRateHz ?: 16_000,
            languages = manifest.languages,
            recognitionMode = manifest.sttRecognitionMode,
        )
    }

    fun textToSpeech(
        manifest: ModelManifest,
        modelDirectory: String,
        artifacts: List<ModelArtifactReference>,
    ): TextToSpeechModelReference {
        require(AiCapability.TEXT_TO_SPEECH in manifest.capabilities) {
            "This installed model is not a compatible local text-to-speech model."
        }
        return TextToSpeechModelReference(
            modelId = manifest.modelId,
            displayName = manifest.displayName,
            engineId = manifest.engineId,
            profileType = manifest.profileType,
            modelDirectory = modelDirectory,
            sampleRateHz = manifest.sampleRateHz ?: 44_100,
            languages = manifest.languages,
            speakerCount = manifest.speakerCount,
            voiceMode = manifest.ttsVoiceMode,
            supportedControls = manifest.ttsControls,
            artifacts = artifacts,
        )
    }

    fun androidSpeechRecognizer() = SpeechToTextModelReference(
        modelId = BuiltInSpeechToTextModels.ANDROID_SPEECH_RECOGNIZER,
        displayName = "Android On-device SpeechRecognizer",
        engineId = SystemModelProfileKeys.ANDROID_SPEECH_RECOGNIZER.engineId,
        profileType = SystemModelProfileKeys.ANDROID_SPEECH_RECOGNIZER.profileId,
        modelDirectory = "",
        artifacts = emptyList(),
        sampleRateHz = 16_000,
        languages = linkedSetOf("English", "Russian", "Chinese", "Japanese", "Korean", "Cantonese"),
        recognitionMode = SttRecognitionMode.STREAMING,
    )

    fun androidTextToSpeech() = TextToSpeechModelReference(
        modelId = BuiltInTextToSpeechModels.ANDROID_TEXT_TO_SPEECH,
        displayName = "Android On-device TextToSpeech",
        engineId = SystemModelProfileKeys.ANDROID_TEXT_TO_SPEECH.engineId,
        profileType = SystemModelProfileKeys.ANDROID_TEXT_TO_SPEECH.profileId,
        modelDirectory = "",
        sampleRateHz = 0,
        languages = linkedSetOf("English", "Russian", "Chinese"),
        speakerCount = null,
        voiceMode = TtsVoiceMode.PLATFORM,
        supportedControls = setOf(TtsControl.LANGUAGE, TtsControl.SPEAKER, TtsControl.SPEECH_RATE),
    )
}
