package com.dmitriim.localaiplayground.source.models.validation

import android.util.Log
import com.dmitriim.localaiplayground.ai.api.model.ModelAdapterRegistry
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.library.ModelValidationState
import com.dmitriim.localaiplayground.core.model.manifest.ModelManifest
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File

/** Validates the manifest, its files, and the runtime-specific metadata in one place. */
@Inject
@SingleIn(AppScope::class)
class ModelFileValidator(
    private val adapters: ModelAdapterRegistry,
) {
    fun validate(
        manifest: ModelManifest,
        directory: File,
        verifyChecksums: Boolean = true,
    ): Pair<ModelValidationState, String?> {
        Log.i(TAG, "Model file validation started: modelId=${manifest.modelId.value}, profile=${manifest.profileType}, fileCount=${manifest.files.size}, verifyChecksums=$verifyChecksums")
        if (!directory.isDirectory) return validationFailure(manifest, ModelValidationState.MISSING_FILES, "The installed model directory is missing.")
        manifest.files.filter { it.required }.forEach { spec ->
            val file = File(directory, spec.relativePath)
            val present = if (spec.directory) file.isDirectory && file.canRead() else file.isFile && file.canRead()
            if (!present) return validationFailure(manifest, ModelValidationState.MISSING_FILES, "Missing ${spec.relativePath}.")
            if (spec.directory) return@forEach
            if (spec.expectedBytes != null && file.length() != spec.expectedBytes) {
                return validationFailure(manifest, ModelValidationState.INVALID, "${spec.relativePath} has an unexpected size.")
            }
            if (verifyChecksums && spec.sha256 != null && !file.sha256().equals(spec.sha256, ignoreCase = true)) {
                return validationFailure(manifest, ModelValidationState.INVALID, "${spec.relativePath} has an unexpected checksum.")
            }
        }
        val adapter = adapters.find(manifest.profileType)
            ?: return validationFailure(manifest, ModelValidationState.INCOMPATIBLE, "No packaged adapter supports ${manifest.profileType.value}.")
        if (adapter.engineId != manifest.engineId) {
            return validationFailure(manifest, ModelValidationState.INCOMPATIBLE, "${manifest.profileType.value} requires ${adapter.engineId.value}.")
        }
        val result = adapter.validate(manifest, directory)
        return if (result.valid) {
            Log.i(TAG, "Model file validation passed: modelId=${manifest.modelId.value}")
            ModelValidationState.READY to null
        } else validationFailure(manifest, ModelValidationState.INVALID, result.message ?: "Engine metadata validation failed.")
    }

    fun enrichChecksums(manifest: ModelManifest, directory: File): ModelManifest = manifest.copy(
        files = manifest.files.map { spec ->
            val file = File(directory, spec.relativePath)
            if (!file.isFile || spec.directory) spec else spec.copy(
                expectedBytes = spec.expectedBytes ?: file.length(),
                sha256 = spec.sha256 ?: file.sha256(),
            )
        },
    )

    fun hasValidatorFor(manifest: ModelManifest): Boolean = adapters.find(manifest.profileType)?.engineId == manifest.engineId

    private fun validationFailure(
        manifest: ModelManifest,
        state: ModelValidationState,
        message: String,
    ): Pair<ModelValidationState, String> {
        Log.w(TAG, "Model file validation failed: modelId=${manifest.modelId.value}, state=$state, message=$message")
        return state to message
    }

    private companion object {
        const val TAG = "AiP123Models"
    }
}

internal fun File.totalFileBytes(): Long = walkTopDown().filter { it.isFile }.sumOf { it.length() }
