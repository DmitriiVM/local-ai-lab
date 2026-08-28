package com.dmitriim.localailab.ai.llamacpp

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.model.RuntimeValidationResult
import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelFormat
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.manifest.ModelProfileKey
import dev.zacsweers.metro.Inject
import java.io.File
import java.io.RandomAccessFile

internal interface LlamaCppProfile : ModelRuntimeProfile

@Inject
class LlamaCppRuntimeProfile : LlamaCppProfile {
    override val key = ModelProfileKey(EngineId("llama.cpp"), ModelProfileIds.LLM)
    override val displayName = "GGUF chat model"
    override val capabilities = setOf(AiCapability.CHAT)
    override fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult = runCatching {
        require(manifest.engineId == key.engineId && manifest.profileType == key.profileId)
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
