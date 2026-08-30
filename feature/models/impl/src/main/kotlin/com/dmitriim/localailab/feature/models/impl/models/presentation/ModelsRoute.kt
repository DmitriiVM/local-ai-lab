package com.dmitriim.localailab.feature.models.impl.models.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localailab.core.navigation.AppNavigator
import com.dmitriim.localailab.feature.models.api.navigation.ModelDetailsDestination
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
