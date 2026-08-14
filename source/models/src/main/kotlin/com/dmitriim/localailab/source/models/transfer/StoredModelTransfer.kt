package com.dmitriim.localailab.source.models.transfer

import com.dmitriim.localailab.core.model.library.ModelTransferNetworkPolicy

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
)
