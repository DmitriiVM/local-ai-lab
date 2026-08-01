package com.dmitriim.localaiplayground.source.models.transfer

import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.library.ModelTransferState
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Shared in-process transfer state used by the library and transfer workflows. */
@Inject
@SingleIn(AppScope::class)
class ModelTransferStateStore {
    private val mutableTransfers = MutableStateFlow<Map<ModelId, ModelTransferState>>(emptyMap())
    val transfers: Flow<Map<ModelId, ModelTransferState>> = mutableTransfers.asStateFlow()

    fun update(transform: (Map<ModelId, ModelTransferState>) -> Map<ModelId, ModelTransferState>) {
        mutableTransfers.update(transform)
    }

    fun stateFor(modelId: ModelId): ModelTransferState? = mutableTransfers.value[modelId]
}
