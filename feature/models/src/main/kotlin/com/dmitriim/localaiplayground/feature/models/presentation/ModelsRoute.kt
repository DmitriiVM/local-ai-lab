package com.dmitriim.localaiplayground.feature.models.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localaiplayground.feature.models.presentation.ui.ModelsScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun ModelsRoute(
    viewModel: ModelsViewModel = metroViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ModelsScreen(
        uiState = uiState,
        onDownload = viewModel::download,
        onCancelTransfer = viewModel::cancelTransfer,
        onDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
    )
}
