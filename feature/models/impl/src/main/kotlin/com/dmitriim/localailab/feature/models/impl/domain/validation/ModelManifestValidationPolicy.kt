package com.dmitriim.localailab.feature.models.impl.domain.validation

import com.dmitriim.localailab.ai.api.model.manifest.ModelFileSpec
import com.dmitriim.localailab.feature.models.api.domain.library.ModelValidationState

/** Pure rules that classify installed-model validation outcomes. */
internal object ModelManifestValidationPolicy {
    fun missingModelDirectory() = failure(
        state = ModelValidationState.MISSING_FILES,
        message = "The installed model directory is missing.",
    )

    fun missingRequiredFile(spec: ModelFileSpec, isPresent: Boolean): ModelValidationResult? = when {
        !spec.required || isPresent -> null
        else -> failure(ModelValidationState.MISSING_FILES, "Missing ${spec.relativePath}.")
    }

    fun unexpectedFileSize(spec: ModelFileSpec, actualBytes: Long): ModelValidationResult? = when {
        spec.expectedBytes == null || spec.expectedBytes == actualBytes -> null
        else -> failure(ModelValidationState.INVALID, "${spec.relativePath} has an unexpected size.")
    }

    fun unexpectedChecksum(spec: ModelFileSpec, matchesExpectedChecksum: Boolean): ModelValidationResult? = when {
        matchesExpectedChecksum -> null
        else -> failure(ModelValidationState.INVALID, "${spec.relativePath} has an unexpected checksum.")
    }

    fun unsupportedRuntime(engineId: String, profileType: String) = failure(
        state = ModelValidationState.INCOMPATIBLE,
        message = "No packaged runtime profile supports $engineId/$profileType.",
    )

    fun invalidRuntimeMetadata(message: String?) = failure(
        state = ModelValidationState.INVALID,
        message = message ?: "Engine metadata validation failed.",
    )

    fun ready() = ModelValidationResult(ModelValidationState.READY)

    private fun failure(state: ModelValidationState, message: String) = ModelValidationResult(state, message)
}
