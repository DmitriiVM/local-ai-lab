package com.dmitriim.localaiplayground.ai.sherpa

import com.dmitriim.localaiplayground.ai.api.ModelRuntimeValidator
import com.dmitriim.localaiplayground.ai.api.RuntimeValidationResult
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.core.model.ModelManifest
import com.dmitriim.localaiplayground.core.model.RuntimeProfileType
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import java.io.File

@Inject
@ContributesIntoSet(AppScope::class)
class SherpaModelRuntimeValidator : ModelRuntimeValidator {
    override val engineId = EngineId("sherpa-onnx")

    override fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult = runCatching {
        val missing = manifest.files.filter { it.required }
            .map { File(directory, it.relativePath) }
            .filterNot { it.isFile && it.canRead() }
        require(missing.isEmpty()) { "Missing required files: ${missing.joinToString { it.name }}" }
        require(manifest.profileType in setOf(
            RuntimeProfileType.WHISPER_STT,
            RuntimeProfileType.SILERO_VAD,
            RuntimeProfileType.SUPERTONIC_TTS,
        )) { "Unsupported sherpa-onnx profile: ${manifest.profileType}" }
    }.fold(
        onSuccess = { RuntimeValidationResult(valid = true) },
        onFailure = { RuntimeValidationResult(valid = false, message = it.message) },
    )
}
