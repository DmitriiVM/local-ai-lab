package com.dmitriim.localailab.ai.llamacpp

import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.model.RuntimeValidationResult
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileKey
import dev.zacsweers.metro.Inject
import java.io.File
import java.io.RandomAccessFile

internal interface LlamaCppProfile : ModelRuntimeProfile

private val llamaCppProfileId = ModelProfileId("LLM")

@Inject
class LlamaCppRuntimeProfile : LlamaCppProfile {
    override val key = ModelProfileKey(EngineId("llama.cpp"), llamaCppProfileId)
    override fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult = runCatching {
        val specification = manifest.files.singleOrNull {
            it.required && it.role == ModelFileRoles.PRIMARY_MODEL && !it.directory
        } ?: error("The GGUF manifest does not declare one primary file.")
        val file = File(directory, specification.relativePath)
        require(file.isFile && file.canRead()) { "The GGUF model file is not readable." }
        RandomAccessFile(file, "r").use { input ->
            val signature = ByteArray(4)
            input.readFully(signature)
            require(signature.toString(Charsets.US_ASCII) == "GGUF") {
                "The selected file is not a GGUF model."
            }
        }
    }.fold(
        onSuccess = { RuntimeValidationResult(true) },
        onFailure = { RuntimeValidationResult(false, it.message) },
    )
}
