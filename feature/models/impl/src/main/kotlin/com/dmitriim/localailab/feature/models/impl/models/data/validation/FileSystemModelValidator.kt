package com.dmitriim.localailab.feature.models.impl.models.data.validation

import android.util.Log
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileKey
import com.dmitriim.localailab.ai.runtime.model.ModelRuntimeProfileRegistry
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.feature.models.impl.models.domain.validation.ModelManifestValidationPolicy
import com.dmitriim.localailab.feature.models.impl.models.domain.validation.ModelValidationResult
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File

/** Validates the manifest, its files, and the runtime-specific metadata in one place. */
@Inject
@SingleIn(AppScope::class)
class FileSystemModelValidator(
    private val profiles: ModelRuntimeProfileRegistry,
) {
    fun validate(
        manifest: ModelManifest,
        directory: File,
        verifyChecksums: Boolean = true,
    ): ModelValidationResult {
        Log.i(
            TAG,
            "Model file validation started: modelId=${manifest.modelId.value}, " +
                "profile=${manifest.profileType}, fileCount=${manifest.files.size}, " +
                "verifyChecksums=$verifyChecksums",
        )
        if (!directory.isDirectory) {
            return validationFailure(
                manifest,
                ModelManifestValidationPolicy.missingModelDirectory(),
            )
        }
        manifest.files.filter { it.required }.forEach { spec ->
            val file = File(directory, spec.relativePath)
            val present = if (spec.directory) file.isDirectory && file.canRead() else file.isFile && file.canRead()
            ModelManifestValidationPolicy.missingRequiredFile(spec, present)?.let { result ->
                return validationFailure(manifest, result)
            }
            if (spec.directory) return@forEach
            ModelManifestValidationPolicy.unexpectedFileSize(spec, file.length())?.let { result ->
                return validationFailure(manifest, result)
            }
            if (verifyChecksums && spec.sha256 != null) {
                val matchesExpectedChecksum = file.sha256().equals(spec.sha256, ignoreCase = true)
                ModelManifestValidationPolicy.unexpectedChecksum(spec, matchesExpectedChecksum)?.let { result ->
                    return validationFailure(manifest, result)
                }
            }
        }
        val profile = profiles.runtimeProfile(ModelProfileKey(manifest.engineId, manifest.profileType))
        if (profile == null) {
            return validationFailure(
                manifest,
                ModelManifestValidationPolicy.unsupportedRuntime(manifest.engineId.value, manifest.profileType.value),
            )
        }
        val result = profile.validate(manifest, directory)
        return if (result.valid) {
            Log.i(TAG, "Model file validation passed: modelId=${manifest.modelId.value}")
            ModelManifestValidationPolicy.ready()
        } else {
            validationFailure(manifest, ModelManifestValidationPolicy.invalidRuntimeMetadata(result.message))
        }
    }

    fun enrichChecksums(manifest: ModelManifest, directory: File): ModelManifest = manifest.copy(
        files = manifest.files.map { spec ->
            val file = File(directory, spec.relativePath)
            if (!file.isFile || spec.directory) {
                spec
            } else {
                spec.copy(
                    expectedBytes = spec.expectedBytes ?: file.length(),
                    sha256 = spec.sha256 ?: file.sha256(),
                )
            }
        },
    )

    fun hasValidatorFor(manifest: ModelManifest): Boolean = profiles.runtimeProfile(
        ModelProfileKey(manifest.engineId, manifest.profileType),
    ) != null

    private fun validationFailure(
        manifest: ModelManifest,
        result: ModelValidationResult,
    ): ModelValidationResult {
        Log.w(
            TAG,
            "Model file validation failed: modelId=${manifest.modelId.value}, " +
                "state=${result.state}, message=${result.message}",
        )
        return result
    }

    private companion object {
        const val TAG = "AiP123Models"
    }
}

internal fun File.totalFileBytes(): Long = walkTopDown().filter { it.isFile }.sumOf { it.length() }
