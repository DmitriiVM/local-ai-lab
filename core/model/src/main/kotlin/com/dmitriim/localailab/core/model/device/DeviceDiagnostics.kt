package com.dmitriim.localailab.core.model.device

import com.dmitriim.localailab.core.model.capability.AiCapability

data class DeviceDiagnostics(
    val modelDirectoryWritable: Boolean,
    val availableTemporaryBytes: Long,
    val installedFilesValid: Boolean,
    val offlineReadyCapabilities: Set<AiCapability>,
    val detail: List<String>,
)
