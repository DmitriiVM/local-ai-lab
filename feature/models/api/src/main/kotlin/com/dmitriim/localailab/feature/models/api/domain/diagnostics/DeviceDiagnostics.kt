package com.dmitriim.localailab.feature.models.api.domain.diagnostics

import com.dmitriim.localailab.ai.api.capability.AiCapability

data class DeviceDiagnostics(
    val modelDirectoryWritable: Boolean,
    val availableTemporaryBytes: Long,
    val installedFilesValid: Boolean,
    val offlineReadyCapabilities: Set<AiCapability>,
    val detail: List<String>,
)
