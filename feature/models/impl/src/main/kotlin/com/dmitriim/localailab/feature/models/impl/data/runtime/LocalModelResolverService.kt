package com.dmitriim.localailab.feature.models.impl.data.runtime

import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.feature.models.api.domain.library.BuiltInSpeechToTextModels
import com.dmitriim.localailab.feature.models.api.domain.library.BuiltInTextToSpeechModels
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.model.runtime.ModelArtifactReference
import com.dmitriim.localailab.feature.models.api.domain.runtime.LocalModelResolver
import com.dmitriim.localailab.feature.models.impl.data.library.InstalledModelService
import com.dmitriim.localailab.feature.models.impl.domain.runtime.ModelRuntimeReferenceFactory
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
    override suspend fun resolveChatModel(modelId: ModelId) = runCatching {
        val (manifest, directory) = installedModels.requireReadyModel(modelId)
        ModelRuntimeReferenceFactory.chat(manifest, manifest.resolvedArtifacts(directory))
    }

    override suspend fun resolveSpeechToTextModel(modelId: ModelId) = runCatching {
        if (modelId == BuiltInSpeechToTextModels.ANDROID_SPEECH_RECOGNIZER) {
            return@runCatching ModelRuntimeReferenceFactory.androidSpeechRecognizer()
        }
        val (manifest, directory) = installedModels.requireReadyModel(modelId)
        ModelRuntimeReferenceFactory.speechToText(
            manifest = manifest,
            modelDirectory = directory.absolutePath,
            artifacts = manifest.resolvedArtifacts(directory),
        )
    }

    override suspend fun resolveTextToSpeechModel(modelId: ModelId) = runCatching {
        if (modelId == BuiltInTextToSpeechModels.ANDROID_TEXT_TO_SPEECH) {
            return@runCatching ModelRuntimeReferenceFactory.androidTextToSpeech()
        }
        val (manifest, directory) = installedModels.requireReadyModel(modelId)
        ModelRuntimeReferenceFactory.textToSpeech(
            manifest = manifest,
            modelDirectory = directory.absolutePath,
            artifacts = manifest.resolvedArtifacts(directory),
        )
    }

    private fun com.dmitriim.localailab.core.model.manifest.ModelManifest.resolvedArtifacts(
        directory: File,
    ) = files.mapNotNull { specification ->
        val artifact = File(directory, specification.relativePath)
        if (!specification.required && !artifact.exists()) return@mapNotNull null
        ModelArtifactReference(
            role = specification.role,
            path = artifact.absolutePath,
            directory = specification.directory,
            relativePath = specification.relativePath,
        )
    }
}
