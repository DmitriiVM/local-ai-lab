package com.dmitriim.localailab.core.model.runtime

import com.dmitriim.localailab.core.model.manifest.ModelFileRole

/** An app-visible artifact resolved for use by a model runtime. */
data class ModelArtifactReference(
    val role: ModelFileRole,
    val path: String,
    val directory: Boolean = false,
    val relativePath: String = java.io.File(path).name,
) {
    init {
        require(path.isNotBlank()) { "A model artifact path must not be blank." }
    }
}

class ModelArtifacts(private val artifacts: List<ModelArtifactReference>) {
    fun require(role: ModelFileRole): ModelArtifactReference = requireNotNull(
        artifacts.singleOrNull { it.role == role },
    ) { "Expected exactly one ${role.value} model artifact." }

    fun all(role: ModelFileRole): List<ModelArtifactReference> = artifacts.filter { it.role == role }

    fun requirePath(relativePath: String): ModelArtifactReference = requireNotNull(
        artifacts.singleOrNull { it.relativePath == relativePath },
    ) { "Missing $relativePath model artifact." }

    fun asList(): List<ModelArtifactReference> = artifacts
}
