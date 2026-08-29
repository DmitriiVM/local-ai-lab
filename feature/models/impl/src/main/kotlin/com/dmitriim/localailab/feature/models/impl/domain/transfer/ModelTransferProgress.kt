package com.dmitriim.localailab.feature.models.impl.domain.transfer

import com.dmitriim.localailab.ai.api.model.manifest.ModelId

internal data class ModelTransferProgress(
    val modelId: ModelId,
    val executionGeneration: Long,
    val completedBytes: Long,
    val totalBytes: Long,
    val recordedAtElapsedMs: Long,
)
