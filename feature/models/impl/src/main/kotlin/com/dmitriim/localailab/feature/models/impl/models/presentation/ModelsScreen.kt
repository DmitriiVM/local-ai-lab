package com.dmitriim.localailab.feature.models.impl.models.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.core.ui.layout.LocalAppDimensions

@Composable
fun ModelsScreen(
    uiState: ModelsUiState,
    onOpenDetails: (ModelId) -> Unit,
    onDownload: (ModelId) -> Unit,
    onPauseTransfer: (ModelId) -> Unit,
    onResumeOnWifi: (ModelId) -> Unit,
    onCancelTransfer: (ModelId) -> Unit,
    onDelete: (ModelId) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    var typeFilter by rememberSaveable { mutableStateOf(ModelTypeFilter.ALL) }
    var runtimeFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var installationFilter by rememberSaveable { mutableStateOf(ModelInstallationFilter.ALL) }
    val allModelItems = uiState.toModelListItems()
    val runtimeIds = allModelItems
        .map { it.manifest.engineId }
        .distinct()
        .sortedBy { it.value }
    val modelItems = allModelItems
        .filter { typeFilter.matches(it.manifest) }
        .filter { runtimeFilter == null || it.manifest.engineId.value == runtimeFilter }
        .filter { installationFilter.matches(it) }
    ModelsList(
        modelItems = modelItems,
        runtimeIds = runtimeIds,
        typeFilter = typeFilter,
        runtimeFilter = runtimeFilter,
        installationFilter = installationFilter,
        transfers = uiState.transfers,
        credentialStatus = uiState.huggingFaceCredentialStatus,
        onOpenDetails = onOpenDetails,
        onDownload = onDownload,
        onPauseTransfer = onPauseTransfer,
        onResumeOnWifi = onResumeOnWifi,
        onCancelTransfer = onCancelTransfer,
        onDelete = onDelete,
        onTypeFilterChange = { typeFilter = it },
        onRuntimeFilterChange = { runtimeFilter = it },
        onInstallationFilterChange = { installationFilter = it },
        dimensions = dimensions,
    )
    DeleteModelDialog(
        model = uiState.pendingDelete,
        onConfirmDelete = onConfirmDelete,
        onCancelDelete = onCancelDelete,
    )
}
