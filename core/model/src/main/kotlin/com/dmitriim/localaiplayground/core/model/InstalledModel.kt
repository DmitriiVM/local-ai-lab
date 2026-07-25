package com.dmitriim.localaiplayground.core.model

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
    val loaded: Boolean = false,
    val lastUsedAtEpochMs: Long? = null,
)

enum class ModelCompatibilityState {
    COMPATIBLE,
    INCOMPATIBLE,
    ADVISORY_WARNING,
}

data class ModelCompatibility(
    val state: ModelCompatibilityState,
    val reasons: List<String>,
)
