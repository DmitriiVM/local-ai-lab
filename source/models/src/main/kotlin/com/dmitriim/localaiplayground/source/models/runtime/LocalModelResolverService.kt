package com.dmitriim.localaiplayground.source.models.runtime

import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.AiCapability
import com.dmitriim.localaiplayground.core.model.ChatModelReference
import com.dmitriim.localaiplayground.core.model.LocalModelResolver
import com.dmitriim.localaiplayground.core.model.ModelFileRoles
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.SpeechToTextModelReference
import com.dmitriim.localaiplayground.core.model.TextToSpeechModelReference
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
        val primary = requireNotNull(manifest.files.firstOrNull { it.role == ModelFileRoles.PRIMARY_MODEL }) {
            "The chat model does not declare a primary GGUF file."
        }
        ChatModelReference(
            modelId = modelId,
            displayName = manifest.displayName,
            profileType = manifest.profileType,
            modelPath = File(directory, primary.relativePath).absolutePath,
            defaultContextSize = manifest.contextSize ?: 512,
        )
    }

    override suspend fun resolveSpeechToTextModel(modelId: ModelId): Result<SpeechToTextModelReference> = runCatching {
        val (manifest, directory) = installedModels.requireReadyModel(modelId)
        require(AiCapability.SPEECH_TO_TEXT in manifest.capabilities) {
            "This installed model is not a compatible local speech-to-text model."
        }
        SpeechToTextModelReference(
            modelId = modelId,
            displayName = manifest.displayName,
            profileType = manifest.profileType,
            modelDirectory = directory.absolutePath,
            sampleRateHz = manifest.sampleRateHz ?: 16_000,
            languages = manifest.languages,
        )
    }

    override suspend fun resolveTextToSpeechModel(modelId: ModelId): Result<TextToSpeechModelReference> = runCatching {
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
