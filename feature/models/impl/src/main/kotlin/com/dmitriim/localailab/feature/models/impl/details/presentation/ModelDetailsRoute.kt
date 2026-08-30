package com.dmitriim.localailab.feature.models.impl.details.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.feature.models.impl.details.presentation.ModelDetailsScreen
import com.dmitriim.localailab.feature.models.impl.models.presentation.ModelsViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun ModelDetailsRoute(
    modelId: ModelId,
    onNavigateBack: () -> Unit,
    viewModel: ModelsViewModel = metroViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(modelId) {
        viewModel.selectModel(modelId)
    }
    val manifest = uiState.catalog.firstOrNull { it.manifest.modelId == modelId }?.manifest
        ?: uiState.installed.firstOrNull { it.manifest.modelId == modelId }?.manifest
    LaunchedEffect(manifest) {
        manifest?.let(viewModel::checkCompatibility)
    }
    ModelDetailsScreen(
        modelId = modelId,
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onDownload = viewModel::download,
        onPauseTransfer = viewModel::pauseTransfer,
        onResumeOnWifi = viewModel::resumeOnWifi,
        onResumeOnAnyNetwork = viewModel::resumeOnAnyNetwork,
        onCancelTransfer = viewModel::cancelTransfer,
        onValidate = viewModel::validate,
        onDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
        onRequestHuggingFaceToken = viewModel::requestHuggingFaceToken,
        onSaveHuggingFaceToken = viewModel::saveHuggingFaceTokenAndDownload,
        onDismissHuggingFaceToken = viewModel::dismissHuggingFaceToken,
    )
}
