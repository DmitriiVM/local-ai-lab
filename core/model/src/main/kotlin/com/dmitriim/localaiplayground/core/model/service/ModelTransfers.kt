package com.dmitriim.localaiplayground.core.model.service

import com.dmitriim.localaiplayground.core.model.library.CatalogModel
import com.dmitriim.localaiplayground.core.model.library.ModelTransferNetworkPolicy
import com.dmitriim.localaiplayground.core.model.library.ModelTransferState
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import kotlinx.coroutines.flow.Flow

/** Exposes the bundled catalog and its persisted download operations. */
interface ModelTransfers {
    val catalog: Flow<List<CatalogModel>>
    val transfers: Flow<Map<ModelId, ModelTransferState>>

    suspend fun download(
        modelId: ModelId,
        networkPolicy: ModelTransferNetworkPolicy = ModelTransferNetworkPolicy.WIFI_ONLY,
    ): Result<Unit>
    suspend fun pauseTransfer(modelId: ModelId)
    suspend fun resumeTransfer(
        modelId: ModelId,
        networkPolicy: ModelTransferNetworkPolicy,
    ): Result<Unit>
    suspend fun cancelTransfer(modelId: ModelId)
}
