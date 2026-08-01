package com.dmitriim.localaiplayground.source.models.runtime

import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.library.BuiltInSpeechToTextModels
import com.dmitriim.localaiplayground.core.model.library.BuiltInTextToSpeechModels
import com.dmitriim.localaiplayground.core.model.manifest.ModelFileRoles
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileIds
import com.dmitriim.localaiplayground.core.model.manifest.SttRecognitionMode
import com.dmitriim.localaiplayground.core.model.manifest.TtsControl
import com.dmitriim.localaiplayground.core.model.manifest.TtsVoiceMode
import com.dmitriim.localaiplayground.core.model.runtime.ChatModelReference
import com.dmitriim.localaiplayground.core.model.runtime.ModelArtifactReference
import com.dmitriim.localaiplayground.core.model.runtime.SpeechToTextModelReference
import com.dmitriim.localaiplayground.core.model.runtime.TextToSpeechModelReference
import com.dmitriim.localaiplayground.core.model.service.LocalModelResolver
import com.dmitriim.localaiplayground.source.models.library.InstalledModelService
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File

/** Resolves runtime-specific references only after revalidating the installed model. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class LocalModelResolverService(
    private val installedModels: InstalledModelService,
) : LocalModelResolver {
    override suspend fun resolveChatModel(modelId: ModelId): Result<ChatModelReference> = runCatching {
        val (manifest, directory) = installedModels.requireReadyModel(modelId)
        require(AiCapability.CHAT in manifest.capabilities) {
            "This installed model is not a compatible local chat model."
        }
        requireNotNull(manifest.files.firstOrNull { it.role == ModelFileRoles.PRIMARY_MODEL }) {
            "The chat model does not declare a primary model artifact."
        }
        val artifacts = manifest.files.mapNotNull { specification ->
            val artifact = File(directory, specification.relativePath)
            if (!specification.required && !artifact.exists()) return@mapNotNull null
            ModelArtifactReference(
                role = specification.role,
                path = artifact.absolutePath,
                directory = specification.directory,
            )
        }
        ChatModelReference.ArtifactBacked(
            modelId = modelId,
            displayName = manifest.displayName,
            engineId = manifest.engineId,
            profileType = manifest.profileType,
            defaultContextSize = manifest.contextSize ?: 512,
            artifacts = artifacts,
        )
    }

    override suspend fun resolveSpeechToTextModel(modelId: ModelId): Result<SpeechToTextModelReference> = runCatching {
        if (modelId == BuiltInSpeechToTextModels.ANDROID_SPEECH_RECOGNIZER) {
            return@runCatching SpeechToTextModelReference(
                modelId = modelId,
                displayName = "Android On-device SpeechRecognizer",
                engineId = EngineId("android-speech-recognizer"),
                profileType = ModelProfileIds.ANDROID_SPEECH_RECOGNIZER_STT,
                modelDirectory = "",
                files = emptyMap(),
                sampleRateHz = 16_000,
                languages = linkedSetOf("English", "Russian", "Chinese", "Japanese", "Korean", "Cantonese"),
                recognitionMode = SttRecognitionMode.STREAMING,
            )
        }
        val (manifest, directory) = installedModels.requireReadyModel(modelId)
        require(AiCapability.SPEECH_TO_TEXT in manifest.capabilities) {
            "This installed model is not a compatible local speech-to-text model."
        }
        SpeechToTextModelReference(
            modelId = modelId,
            displayName = manifest.displayName,
            engineId = manifest.engineId,
            profileType = manifest.profileType,
            modelDirectory = directory.absolutePath,
            files = manifest.files
                .filter { it.required && !it.directory }
                .associate { it.role to File(directory, it.relativePath).absolutePath },
            sampleRateHz = manifest.sampleRateHz ?: 16_000,
            languages = manifest.languages,
            recognitionMode = manifest.sttRecognitionMode,
        )
    }

    override suspend fun resolveTextToSpeechModel(modelId: ModelId): Result<TextToSpeechModelReference> = runCatching {
        if (modelId == BuiltInTextToSpeechModels.ANDROID_TEXT_TO_SPEECH) {
            return@runCatching TextToSpeechModelReference(
                modelId = modelId,
                displayName = "Android On-device TextToSpeech",
                engineId = EngineId("android-text-to-speech"),
                profileType = ModelProfileIds.ANDROID_TEXT_TO_SPEECH_TTS,
                modelDirectory = "",
                sampleRateHz = 0,
                languages = linkedSetOf("English", "Russian", "Chinese"),
                speakerCount = null,
                voiceMode = TtsVoiceMode.PLATFORM,
                supportedControls = setOf(
                    TtsControl.LANGUAGE,
                    TtsControl.SPEAKER,
                    TtsControl.SPEECH_RATE,
                ),
            )
        }
        val (manifest, directory) = installedModels.requireReadyModel(modelId)
        require(AiCapability.TEXT_TO_SPEECH in manifest.capabilities) {
            "This installed model is not a compatible local text-to-speech model."
        }
        TextToSpeechModelReference(
            modelId = modelId,
            displayName = manifest.displayName,
            engineId = manifest.engineId,
            profileType = manifest.profileType,
            modelDirectory = directory.absolutePath,
            sampleRateHz = manifest.sampleRateHz ?: 44_100,
            languages = manifest.languages,
            speakerCount = manifest.speakerCount,
            voiceMode = manifest.ttsVoiceMode,
            supportedControls = manifest.ttsControls,
        )
    }
}
