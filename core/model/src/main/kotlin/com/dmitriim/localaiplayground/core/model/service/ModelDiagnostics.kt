package com.dmitriim.localaiplayground.core.model.service

import com.dmitriim.localaiplayground.core.model.device.DeviceDiagnostics
import com.dmitriim.localaiplayground.core.model.library.ModelCompatibility
import com.dmitriim.localaiplayground.core.model.manifest.ModelManifest

/** Checks model requirements against the device and reports local model health. */
interface ModelDiagnostics {
    suspend fun compatibility(model: ModelManifest): ModelCompatibility
    suspend fun runDiagnostics(): DeviceDiagnostics
}
