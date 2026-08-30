package com.dmitriim.localailab.feature.models.impl.models.data.transfer

import com.dmitriim.localailab.ai.api.model.manifest.ModelId

internal interface ModelDownloadExecutor {
    suspend fun executeScheduledDownload(modelId: ModelId, executionGeneration: Long)
}
