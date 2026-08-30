package com.dmitriim.localailab.feature.models.impl.models.data.transfer

import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferNetworkPolicy

internal data class StoredModelTransfer(
    val modelId: String,
    val catalogVersion: Int,
    val revision: String?,
    val status: PersistedModelTransferStatus,
    val networkPolicy: ModelTransferNetworkPolicy,
    val executionGeneration: Long,
    val completedBytes: Long,
    val totalBytes: Long,
    val currentRelativePath: String?,
    val message: String?,
    val retryAttempt: Int,
    val nextAttemptAtEpochMs: Long,
)
