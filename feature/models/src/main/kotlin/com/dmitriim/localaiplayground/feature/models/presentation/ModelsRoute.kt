package com.dmitriim.localaiplayground.feature.models.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitriim.localaiplayground.feature.models.presentation.ui.ModelsScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun ModelsRoute(
    viewModel: ModelsViewModel = metroViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingImport by remember { mutableStateOf<com.dmitriim.localaiplayground.core.model.RuntimeProfileType?>(null) }
    val documents = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        pendingImport?.let { profile -> viewModel.import(profile, uris.map { it.toString() }) }
        pendingImport = null
    }
    ModelsScreen(
        uiState = uiState,
        onImport = { profile ->
            pendingImport = profile
            documents.launch(arrayOf("application/octet-stream", "application/onnx", "text/plain", "application/json"))
        },
        onDownload = viewModel::download,
        onCancelTransfer = viewModel::cancelTransfer,
        onValidate = viewModel::validate,
        onDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
    )
}
