package com.dmitriim.localailab.source.models.transfer

import com.dmitriim.localailab.core.model.manifest.ModelId

internal interface ModelDownloadExecutor {
    suspend fun executeScheduledDownload(modelId: ModelId, executionGeneration: Long)
}
