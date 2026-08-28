package com.dmitriim.localailab.ai.api.model

import com.dmitriim.localailab.core.model.manifest.ModelFileRole
import com.dmitriim.localailab.core.model.manifest.ModelFormat

/**
 * Runtime-owned import rules for a user-supplied model bundle.
 *
 * The model library uses these rules to construct a manifest before the runtime validates the
 * resulting installation. A null import definition on [ModelRuntimeProfile] means that profile
 * cannot be imported by users.
 */
data class ModelImportDefinition(
    val displayName: String,
    val format: ModelFormat,
    val files: List<ModelImportFileDefinition>,
)

/** One required file or directory shape in a user-imported model bundle. */
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
