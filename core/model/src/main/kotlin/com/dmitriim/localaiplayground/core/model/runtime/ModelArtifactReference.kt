package com.dmitriim.localaiplayground.core.model.runtime

import com.dmitriim.localaiplayground.core.model.manifest.ModelFileRole

/** An app-visible artifact resolved for use by a model runtime. */
data class ModelArtifactReference(
    val role: ModelFileRole,
    val path: String,
    val directory: Boolean = false,
) {
    init {
        require(path.isNotBlank()) { "A model artifact path must not be blank." }
    }
}
