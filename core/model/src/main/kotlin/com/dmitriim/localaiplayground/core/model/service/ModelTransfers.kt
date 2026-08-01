package com.dmitriim.localaiplayground.core.model.service

import com.dmitriim.localaiplayground.core.model.library.CatalogModel
import com.dmitriim.localaiplayground.core.model.library.ModelTransferState
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import kotlinx.coroutines.flow.Flow

/** Exposes the bundled catalog and its persisted download operations. */
interface ModelTransfers {
    val catalog: Flow<List<CatalogModel>>
    val transfers: Flow<Map<ModelId, ModelTransferState>>

    suspend fun download(modelId: ModelId): Result<Unit>
    suspend fun cancelTransfer(modelId: ModelId)
}
