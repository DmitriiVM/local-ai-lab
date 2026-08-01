package com.dmitriim.localaiplayground.core.model.library

import com.dmitriim.localaiplayground.core.model.manifest.ModelManifest
import kotlinx.serialization.Serializable

@Serializable
enum class ModelValidationState {
    READY,
    INVALID,
    MISSING_FILES,
    INCOMPATIBLE,
}

data class InstalledModel(
    val manifest: ModelManifest,
    val localDirectoryName: String,
    val totalBytes: Long,
    val validationState: ModelValidationState,
    val validationMessage: String? = null,
    val lastUsedAtEpochMs: Long? = null,
)
