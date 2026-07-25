package com.dmitriim.localaiplayground.feature.models.presentation

import com.dmitriim.localaiplayground.core.model.CatalogModel
import com.dmitriim.localaiplayground.core.model.InstalledModel
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelTransferState

data class ModelsUiState(
    val installed: List<InstalledModel> = emptyList(),
    val catalog: List<CatalogModel> = emptyList(),
    val transfers: Map<ModelId, ModelTransferState> = emptyMap(),
    val selectedModelId: ModelId? = null,
    val message: String? = null,
    val pendingDelete: InstalledModel? = null,
    val validatingModelIds: Set<ModelId> = emptySet(),
    val validationFeedback: Map<ModelId, ModelValidationFeedback> = emptyMap(),
) {
    val isEmpty: Boolean get() = installed.isEmpty()
}

data class ModelValidationFeedback(
    val message: String,
    val isError: Boolean,
)
