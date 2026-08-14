package com.dmitriim.localailab.core.model.service

import com.dmitriim.localailab.core.model.device.DeviceDiagnostics
import com.dmitriim.localailab.core.model.library.ModelCompatibility
import com.dmitriim.localailab.core.model.manifest.ModelManifest

/** Checks model requirements against the device and reports local model health. */
interface ModelDiagnostics {
    suspend fun compatibility(model: ModelManifest): ModelCompatibility
    suspend fun runDiagnostics(): DeviceDiagnostics
}
