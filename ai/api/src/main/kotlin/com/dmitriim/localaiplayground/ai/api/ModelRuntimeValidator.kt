package com.dmitriim.localaiplayground.ai.api

import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.core.model.ModelManifest
import java.io.File

data class RuntimeValidationResult(
    val valid: Boolean,
    val message: String? = null,
)

/** Engine-owned validation; this never copies a complete model into JVM memory. */
interface ModelRuntimeValidator {
    val engineId: EngineId

    fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult
}
