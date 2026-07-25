package com.dmitriim.localaiplayground.ai.llamacpp

import com.dmitriim.localaiplayground.ai.api.ModelRuntimeValidator
import com.dmitriim.localaiplayground.ai.api.RuntimeValidationResult
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.core.model.ModelManifest
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import java.io.File
import java.io.RandomAccessFile

@Inject
@ContributesIntoSet(AppScope::class)
class LlamaCppModelRuntimeValidator : ModelRuntimeValidator {
    override val engineId = EngineId("llama.cpp")

    override fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult = runCatching {
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
