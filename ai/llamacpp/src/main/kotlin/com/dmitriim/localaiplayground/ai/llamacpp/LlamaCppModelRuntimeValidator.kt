package com.dmitriim.localaiplayground.ai.llamacpp

import com.dmitriim.localaiplayground.ai.api.ModelAdapter
import com.dmitriim.localaiplayground.ai.api.ModelImportDefinition
import com.dmitriim.localaiplayground.ai.api.ModelImportFileDefinition
import com.dmitriim.localaiplayground.ai.api.RuntimeValidationResult
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.core.model.ModelManifest
import com.dmitriim.localaiplayground.core.model.ModelProfileId
import com.dmitriim.localaiplayground.core.model.ModelProfileIds
import com.dmitriim.localaiplayground.core.model.AiCapability
import com.dmitriim.localaiplayground.core.model.ModelFileRoles
import com.dmitriim.localaiplayground.core.model.ModelFormat
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import java.io.File
import java.io.RandomAccessFile

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelAdapter>())
class LlamaCppModelRuntimeValidator : ModelAdapter {
    override val id = "llama-cpp-gguf"
    override val engineId = EngineId("llama.cpp")
    override val profileTypes = setOf(ModelProfileIds.LLM)
    override val capabilities = setOf(AiCapability.CHAT)

    override fun capabilitiesFor(profileType: ModelProfileId) =
        if (profileType in profileTypes) capabilities else emptySet()

    override fun importDefinition(profileType: ModelProfileId) = when (profileType) {
        ModelProfileIds.LLM -> ModelImportDefinition(
            displayName = "GGUF chat model",
            format = ModelFormat.GGUF,
            files = listOf(ModelImportFileDefinition(role = ModelFileRoles.PRIMARY_MODEL, extension = ".gguf")),
        )
        else -> null
    }

    override fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult = runCatching {
        require(manifest.profileType in profileTypes) { "Unsupported llama.cpp profile: ${manifest.profileType.value}" }
        val file = manifest.files.firstOrNull { it.required }
            ?.let { File(directory, it.relativePath) }
            ?: error("The GGUF manifest does not declare a primary file.")
        require(file.isFile && file.canRead()) { "The GGUF model file is not readable." }
        RandomAccessFile(file, "r").use { input ->
            val signature = ByteArray(4)
            input.readFully(signature)
            require(signature.toString(Charsets.US_ASCII) == "GGUF") { "The selected file is not a GGUF model." }
        }
    }.fold(
        onSuccess = { RuntimeValidationResult(valid = true) },
        onFailure = { RuntimeValidationResult(valid = false, message = it.message) },
    )
}
