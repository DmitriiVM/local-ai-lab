package com.dmitriim.localaiplayground.source.models.validation

import com.dmitriim.localaiplayground.ai.api.ModelRuntimeValidator
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.ModelFileSpec
import com.dmitriim.localaiplayground.core.model.ModelManifest
import com.dmitriim.localaiplayground.core.model.ModelValidationState
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File

/** Validates the manifest, its files, and the runtime-specific metadata in one place. */
@Inject
@SingleIn(AppScope::class)
class ModelFileValidator(
    private val validators: Set<ModelRuntimeValidator>,
) {
    fun validate(
        manifest: ModelManifest,
        directory: File,
        verifyChecksums: Boolean = true,
    ): Pair<ModelValidationState, String?> {
        if (!directory.isDirectory) return ModelValidationState.MISSING_FILES to "The installed model directory is missing."
        manifest.files.filter { it.required }.forEach { spec ->
            val file = File(directory, spec.relativePath)
            if (!file.isFile || !file.canRead()) return ModelValidationState.MISSING_FILES to "Missing ${spec.relativePath}."
            if (spec.expectedBytes != null && file.length() != spec.expectedBytes) {
                return ModelValidationState.INVALID to "${spec.relativePath} has an unexpected size."
            }
            if (verifyChecksums && spec.sha256 != null && !file.sha256().equals(spec.sha256, ignoreCase = true)) {
                return ModelValidationState.INVALID to "${spec.relativePath} has an unexpected checksum."
            }
        }
        val validator = validators.firstOrNull { it.engineId == manifest.engineId }
            ?: return ModelValidationState.INCOMPATIBLE to "No ${manifest.engineId.value} validator is packaged."
        val result = validator.validate(manifest, directory)
        return if (result.valid) ModelValidationState.READY to null
        else ModelValidationState.INVALID to (result.message ?: "Engine metadata validation failed.")
    }

    fun enrichChecksums(manifest: ModelManifest, directory: File): ModelManifest = manifest.copy(
        files = manifest.files.map { spec ->
            val file = File(directory, spec.relativePath)
            if (!file.isFile) spec else spec.copy(
                expectedBytes = spec.expectedBytes ?: file.length(),
                sha256 = spec.sha256 ?: file.sha256(),
            )
        },
    )

    fun hasValidatorFor(manifest: ModelManifest): Boolean = validators.any { it.engineId == manifest.engineId }
}

internal fun File.totalFileBytes(): Long = walkTopDown().filter { it.isFile }.sumOf { it.length() }
