package com.dmitriim.localailab.ai.vosk

import com.dmitriim.localailab.ai.api.model.ModelRuntimeAdapter
import com.dmitriim.localailab.ai.api.model.ModelImportDefinition
import com.dmitriim.localailab.ai.api.model.ModelImportFileDefinition
import com.dmitriim.localailab.ai.api.model.RuntimeValidationResult
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelFormat
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.manifest.ModelProfileId
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import java.io.File

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelRuntimeAdapter>())
class VoskModelRuntimeAdapter : ModelRuntimeAdapter {
    override val id = "vosk-stt"
    override val engineId = EngineId("vosk")
    override val profileTypes = setOf(ModelProfileIds.VOSK_STT)
    override val capabilities = setOf(AiCapability.SPEECH_TO_TEXT)

    override fun capabilitiesFor(profileType: ModelProfileId) = if (profileType == ModelProfileIds.VOSK_STT) capabilities else emptySet()

    override fun importDefinition(profileType: ModelProfileId) = if (profileType == ModelProfileIds.VOSK_STT) {
        ModelImportDefinition(
            displayName = "Vosk model directory",
            format = ModelFormat.BINARY,
            files = listOf(
                ModelImportFileDefinition(
                    ModelFileRoles.PRIMARY_MODEL,
                    relativePath = "am",
                    directory = true,
                ),
                ModelImportFileDefinition(
                    ModelFileRoles.CONFIG,
                    relativePath = "conf",
                    directory = true,
                ),
                ModelImportFileDefinition(
                    ModelFileRoles.VOCABULARY,
                    relativePath = "graph",
                    directory = true,
                ),
            ),
        )
    } else {
        null
    }

    override fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult = runCatching {
        require(manifest.profileType == ModelProfileIds.VOSK_STT) {
            "Unsupported Vosk profile."
        }
        listOf("am", "conf", "graph").forEach { name ->
            require(File(directory, name).isDirectory) { "Missing Vosk $name directory." }
        }
    }.fold(
        onSuccess = { RuntimeValidationResult(valid = true) },
        onFailure = { RuntimeValidationResult(valid = false, message = it.message) },
    )
}
