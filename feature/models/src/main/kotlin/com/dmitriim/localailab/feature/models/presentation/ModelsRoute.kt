package com.dmitriim.localailab.feature.models.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.core.navigation.destination.ModelDetailsDestination
import com.dmitriim.localailab.feature.models.presentation.ui.ModelDetailsScreen
import com.dmitriim.localailab.feature.models.presentation.ui.ModelsScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun ModelsRoute(
    navigator: AppNavigator,
    viewModel: ModelsViewModel = metroViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ModelsScreen(
        uiState = uiState,
        onDownload = viewModel::download,
        onPauseTransfer = viewModel::pauseTransfer,
        onResumeOnWifi = viewModel::resumeOnWifi,
        onCancelTransfer = viewModel::cancelTransfer,
        onDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
        onOpenDetails = { modelId ->
            navigator.navigate(ModelDetailsDestination(modelId))
        },
    )
}

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
