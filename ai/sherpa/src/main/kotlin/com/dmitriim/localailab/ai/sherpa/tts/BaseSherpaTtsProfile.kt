package com.dmitriim.localailab.ai.sherpa.tts

import com.dmitriim.localailab.ai.api.model.RuntimeValidationResult
import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.manifest.ModelProfileId
import com.dmitriim.localailab.core.model.manifest.ModelProfileKey
import java.io.File

abstract class BaseSherpaTtsProfile(
    profileId: ModelProfileId,
    final override val displayName: String,
) : SherpaTtsProfile {
    final override val key = ModelProfileKey(EngineId("sherpa-onnx"), profileId)
    final override val capabilities = setOf(AiCapability.TEXT_TO_SPEECH)

    final override fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult = runCatching {
        require(manifest.engineId == key.engineId && manifest.profileType == key.profileId)
        val missing = manifest.files.filter { it.required }
            .map { specification -> specification to File(directory, specification.relativePath) }
            .filterNot { (specification, file) ->
                if (specification.directory) file.isDirectory && file.canRead() else file.isFile && file.canRead()
            }
            .map { it.first.relativePath }
        require(missing.isEmpty()) { "Missing required files: ${missing.joinToString()}" }
    }.fold(
        onSuccess = { RuntimeValidationResult(true) },
        onFailure = { RuntimeValidationResult(false, it.message) },
    )
}
