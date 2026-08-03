package com.dmitriim.localaiplayground.source.models.library

import com.dmitriim.localaiplayground.ai.api.model.ModelImportDefinition
import com.dmitriim.localaiplayground.core.model.manifest.ModelFileSpec
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import java.io.File

/** Validates user-controlled import paths and resolves adapter-defined required files. */
internal object ModelImportPolicy {
    fun destination(root: File, relativePath: String): File {
        require(relativePath.isNotBlank()) { "The selected path is empty." }
        val destination = File(root, relativePath)
        require(destination.canonicalFile.parentFile?.startsAt(root.canonicalFile) == true) {
            "The selected directory contains an unsafe path."
        }
        return destination
    }

    fun isSafeFileName(name: String): Boolean = name.isNotBlank() &&
        !name.contains('/') &&
        !name.contains('\\') &&
        name != "." &&
        name != ".."

    fun roleSpecs(definition: ModelImportDefinition, names: List<String>): List<ModelFileSpec> = definition.files.map { file ->
        val relativePath = when {
            file.directory -> {
                val path = requireNotNull(file.relativePath)
                require(names.any { it.startsWith("$path/") }) { "Missing $path directory." }
                path
            }
            file.relativePath != null -> names.firstOrNull { it == file.relativePath }
                ?: error("Missing ${file.relativePath}. Select all required companion files.")
            else -> names.singleOrNull { it.endsWith(requireNotNull(file.extension), ignoreCase = true) }
                ?: error("Select exactly one ${file.extension} file.")
        }
        ModelFileSpec(relativePath, file.role, directory = file.directory)
    }

    fun directoryName(modelId: ModelId): String = modelId.value.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun File.startsAt(root: File): Boolean = this == root || path.startsWith(root.path + File.separator)
}
