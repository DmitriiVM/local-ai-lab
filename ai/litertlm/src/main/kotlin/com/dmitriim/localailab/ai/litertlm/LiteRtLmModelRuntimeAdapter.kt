package com.dmitriim.localailab.ai.litertlm

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
import com.google.ai.edge.litertlm.Capabilities
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import java.io.File

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelRuntimeAdapter>())
class LiteRtLmModelRuntimeAdapter : ModelRuntimeAdapter {
    override val id = "litert-lm-bundle"
    override val engineId = EngineId("litert-lm")
    override val profileTypes = setOf(ModelProfileIds.LLM)
    override val capabilities = setOf(AiCapability.CHAT)

    override fun capabilitiesFor(profileType: ModelProfileId) = if (profileType in profileTypes) capabilities else emptySet()

    override fun importDefinition(profileType: ModelProfileId) = when (profileType) {
        ModelProfileIds.LLM -> ModelImportDefinition(
            displayName = "LiteRT-LM chat model",
            format = ModelFormat.LITERT_LM,
            files = listOf(
                ModelImportFileDefinition(
                    role = ModelFileRoles.PRIMARY_MODEL,
                    extension = ".litertlm",
                ),
            ),
        )
        else -> null
    }

    override fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult = runCatching {
        require(manifest.engineId == engineId) {
            "Unsupported LiteRT-LM engine: ${manifest.engineId.value}"
        }
        require(manifest.profileType in profileTypes) {
            "Unsupported LiteRT-LM profile: ${manifest.profileType.value}"
        }
        require(manifest.format == ModelFormat.LITERT_LM) {
            "The LiteRT-LM manifest has an incompatible format."
        }
        val specification = manifest.files.firstOrNull {
            it.required && it.role == ModelFileRoles.PRIMARY_MODEL && !it.directory
        } ?: error("The LiteRT-LM manifest does not declare a primary file.")
        val file = File(directory, specification.relativePath)
        require(file.isFile && file.canRead()) { "The LiteRT-LM model file is not readable." }
        require(file.extension.equals("litertlm", ignoreCase = true)) {
            "The selected file is not a .litertlm model bundle."
        }
        Capabilities(file.absolutePath).use { }
    }.fold(
        onSuccess = { RuntimeValidationResult(valid = true) },
        onFailure = { RuntimeValidationResult(valid = false, message = it.message) },
    )
}
