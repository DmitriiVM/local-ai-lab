package com.dmitriim.localailab.ai.litertlm

import com.dmitriim.localailab.ai.api.model.ModelImportDefinition
import com.dmitriim.localailab.ai.api.model.ModelImportFileDefinition
import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.model.RuntimeValidationResult
import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelFormat
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.manifest.ModelProfileKey
import com.google.ai.edge.litertlm.Capabilities
import dev.zacsweers.metro.Inject
import java.io.File

internal interface LiteRtLmProfile : ModelRuntimeProfile

@Inject
class LiteRtLmRuntimeProfile : LiteRtLmProfile {
    override val key = ModelProfileKey(EngineId("litert-lm"), ModelProfileIds.LLM)
    override val displayName = "LiteRT-LM chat model"
    override val capabilities = setOf(AiCapability.CHAT)
    override val importDefinition = ModelImportDefinition(
        displayName = displayName,
        format = ModelFormat.LITERT_LM,
        files = listOf(ModelImportFileDefinition(ModelFileRoles.PRIMARY_MODEL, extension = ".litertlm")),
    )

    override fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult = runCatching {
        require(manifest.engineId == key.engineId && manifest.profileType == key.profileId)
        require(manifest.format == ModelFormat.LITERT_LM)
        val specification = manifest.files.singleOrNull {
            it.required && it.role == ModelFileRoles.PRIMARY_MODEL && !it.directory
        } ?: error("The LiteRT-LM manifest does not declare one primary file.")
        val file = File(directory, specification.relativePath)
        require(file.isFile && file.canRead()) { "The LiteRT-LM model file is not readable." }
        require(file.extension.equals("litertlm", ignoreCase = true)) {
            "The selected file is not a .litertlm model bundle."
        }
        Capabilities(file.absolutePath).use { }
    }.fold(
        onSuccess = { RuntimeValidationResult(true) },
        onFailure = { RuntimeValidationResult(false, it.message) },
    )
}
