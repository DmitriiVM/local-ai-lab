package com.dmitriim.localailab.ai.litertlm

import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.model.RuntimeValidationResult
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.manifest.ModelFormat
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileKey
import com.google.ai.edge.litertlm.Capabilities
import dev.zacsweers.metro.Inject
import java.io.File

internal interface LiteRtLmProfile : ModelRuntimeProfile

private val liteRtLmProfileId = ModelProfileId("LLM")

@Inject
class LiteRtLmRuntimeProfile : LiteRtLmProfile {
    override val key = ModelProfileKey(EngineId("litert-lm"), liteRtLmProfileId)
    override fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult = runCatching {
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
