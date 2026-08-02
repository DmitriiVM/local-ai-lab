package com.dmitriim.localaiplayground.ai.api.model

import com.dmitriim.localaiplayground.core.model.manifest.ModelFileRole
import com.dmitriim.localaiplayground.core.model.manifest.ModelFormat

/** Adapter-owned rules for a user-supplied model bundle. */
data class ModelImportDefinition(
    val displayName: String,
    val format: ModelFormat,
    val files: List<ModelImportFileDefinition>,
)

data class ModelImportFileDefinition(
    val role: ModelFileRole,
    val relativePath: String? = null,
    val extension: String? = null,
    val directory: Boolean = false,
) {
    init {
        require((relativePath == null) != (extension == null)) {
            "An import file must declare one path or extension."
        }
    }
}
