package com.dmitriim.localailab.feature.models.impl.data.transfer

import com.dmitriim.localailab.core.model.manifest.ModelId

internal interface ModelDownloadExecutor {
    suspend fun executeScheduledDownload(modelId: ModelId, executionGeneration: Long)
}
