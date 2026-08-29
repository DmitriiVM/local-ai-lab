package com.dmitriim.localailab.feature.models.api.data

import com.dmitriim.localailab.core.model.library.CatalogModel
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferNetworkPolicy
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferState
import com.dmitriim.localailab.core.model.manifest.ModelId
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
