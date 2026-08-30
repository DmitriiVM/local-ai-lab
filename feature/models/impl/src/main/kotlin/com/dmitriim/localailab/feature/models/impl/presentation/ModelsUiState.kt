package com.dmitriim.localailab.feature.models.impl.presentation

import com.dmitriim.localailab.ai.api.model.library.CatalogModel
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.feature.models.api.data.HuggingFaceCredentialStatus
import com.dmitriim.localailab.feature.models.api.domain.diagnostics.ModelCompatibility
import com.dmitriim.localailab.feature.models.api.domain.library.InstalledModel
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferState

data class ModelsUiState(
    val isModelDataLoaded: Boolean = false,
    val installed: List<InstalledModel> = emptyList(),
    val catalog: List<CatalogModel> = emptyList(),
    val transfers: Map<ModelId, ModelTransferState> = emptyMap(),
    val selectedModelId: ModelId? = null,
    val compatibilityModelId: ModelId? = null,
    val compatibility: ModelCompatibility? = null,
    val isCheckingCompatibility: Boolean = false,
    val compatibilityError: String? = null,
    val message: String? = null,
    val pendingDelete: InstalledModel? = null,
    val validatingModelIds: Set<ModelId> = emptySet(),
    val validationFeedback: Map<ModelId, ModelValidationFeedback> = emptyMap(),
    val huggingFaceCredentialStatus: HuggingFaceCredentialStatus = HuggingFaceCredentialStatus.MISSING,
    val pendingHuggingFaceTokenModelId: ModelId? = null,
    val isSavingHuggingFaceToken: Boolean = false,
    val huggingFaceTokenError: String? = null,
) {
    val isEmpty: Boolean get() = installed.isEmpty()
}

data class ModelValidationFeedback(
    val message: String,
    val isError: Boolean,
)
