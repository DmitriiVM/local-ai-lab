package com.dmitriim.localaiplayground.ai.vosk

import com.dmitriim.localaiplayground.ai.api.model.ModelAdapter
import com.dmitriim.localaiplayground.ai.api.model.ModelImportDefinition
import com.dmitriim.localaiplayground.ai.api.model.ModelImportFileDefinition
import com.dmitriim.localaiplayground.ai.api.model.RuntimeValidationResult
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.manifest.ModelFileRoles
import com.dmitriim.localaiplayground.core.model.manifest.ModelFormat
import com.dmitriim.localaiplayground.core.model.manifest.ModelManifest
import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileId
import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileIds
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import java.io.File

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelAdapter>())
class VoskModelRuntimeValidator : ModelAdapter {
    override val id = "vosk-stt"
    override val engineId = EngineId("vosk")
    override val profileTypes = setOf(ModelProfileIds.VOSK_STT)
    override val capabilities = setOf(AiCapability.SPEECH_TO_TEXT)

    override fun capabilitiesFor(profileType: ModelProfileId) =
        if (profileType == ModelProfileIds.VOSK_STT) capabilities else emptySet()

    override fun importDefinition(profileType: ModelProfileId) =
        if (profileType == ModelProfileIds.VOSK_STT) {
            ModelImportDefinition(
                displayName = "Vosk model directory",
                format = ModelFormat.BINARY,
                files = listOf(
                    ModelImportFileDefinition(ModelFileRoles.PRIMARY_MODEL, relativePath = "am", directory = true),
                    ModelImportFileDefinition(ModelFileRoles.CONFIG, relativePath = "conf", directory = true),
                    ModelImportFileDefinition(ModelFileRoles.VOCABULARY, relativePath = "graph", directory = true),
                ),
            )
        } else {
            null
        }

    override fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult = runCatching {
        require(manifest.profileType == ModelProfileIds.VOSK_STT) { "Unsupported Vosk profile." }
        listOf("am", "conf", "graph").forEach { name ->
            require(File(directory, name).isDirectory) { "Missing Vosk $name directory." }
        }
    }.fold(
        onSuccess = { RuntimeValidationResult(valid = true) },
        onFailure = { RuntimeValidationResult(valid = false, message = it.message) },
    )
}
