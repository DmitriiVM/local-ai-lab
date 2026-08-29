package com.dmitriim.localailab.feature.models.api.domain.diagnostics

import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest

/** Checks model requirements against the device and reports local model health. */
interface ModelDiagnostics {
    suspend fun compatibility(model: ModelManifest): ModelCompatibility
    suspend fun runDiagnostics(): DeviceDiagnostics
}
