package com.dmitriim.localailab.ai.sherpa.stt

import com.dmitriim.localailab.ai.api.model.RuntimeValidationResult
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.manifest.ModelProfileId
import com.dmitriim.localailab.core.model.manifest.ModelProfileKey
import java.io.File

abstract class BaseSherpaSttProfile(
    profileId: ModelProfileId,
) : SherpaSttProfile {
    final override val key = ModelProfileKey(EngineId("sherpa-onnx"), profileId)

    final override fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult = runCatching {
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
