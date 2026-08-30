package com.dmitriim.localailab.feature.models.impl.details.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.component.HuggingFaceTokenDialog
import com.dmitriim.localailab.core.ui.layout.LocalAppDimensions
import com.dmitriim.localailab.feature.models.impl.models.presentation.ModelsUiState

@Composable
fun ModelDetailsScreen(
    modelId: ModelId,
    uiState: ModelsUiState,
    onNavigateBack: () -> Unit,
    onDownload: (ModelId) -> Unit,
    onPauseTransfer: (ModelId) -> Unit,
    onResumeOnWifi: (ModelId) -> Unit,
    onResumeOnAnyNetwork: (ModelId) -> Unit,
    onCancelTransfer: (ModelId) -> Unit,
    onValidate: (ModelId) -> Unit,
    onDelete: (ModelId) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onRequestHuggingFaceToken: (ModelId) -> Unit,
    onSaveHuggingFaceToken: (String) -> Unit,
    onDismissHuggingFaceToken: () -> Unit,
) {
    val catalogModel = uiState.catalog.firstOrNull { it.manifest.modelId == modelId }
    val installedModel = uiState.installed.firstOrNull { it.manifest.modelId == modelId }
    val manifest = catalogModel?.manifest ?: installedModel?.manifest

    when {
        manifest != null -> ModelDetailsContent(
            manifest = manifest,
            catalogModel = catalogModel,
            installedModel = installedModel,
            transfer = uiState.transfers[modelId],
            uiState = uiState,
            onDownload = { onDownload(modelId) },
            onPauseTransfer = { onPauseTransfer(modelId) },
            onResumeOnWifi = { onResumeOnWifi(modelId) },
            onResumeOnAnyNetwork = { onResumeOnAnyNetwork(modelId) },
            onCancelTransfer = { onCancelTransfer(modelId) },
            onValidate = { onValidate(modelId) },
            onDelete = { onDelete(modelId) },
            onRequestHuggingFaceToken = { onRequestHuggingFaceToken(modelId) },
        )
        !uiState.isModelDataLoaded -> ModelDetailsLoading()
        else -> ModelUnavailable(uiState.message, onNavigateBack)
    }
    ModelDetailsDialogs(
        modelId = modelId,
        uiState = uiState,
        onConfirmDelete = onConfirmDelete,
        onCancelDelete = onCancelDelete,
        onSaveHuggingFaceToken = onSaveHuggingFaceToken,
        onDismissHuggingFaceToken = onDismissHuggingFaceToken,
    )
}

@Composable
private fun ModelDetailsDialogs(
    modelId: ModelId,
    uiState: ModelsUiState,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onSaveHuggingFaceToken: (String) -> Unit,
    onDismissHuggingFaceToken: () -> Unit,
) {
    uiState.pendingDelete?.takeIf { it.manifest.modelId == modelId }?.let { model ->
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = {
                Text(stringResource(CoreUiR.string.models_model_details_screen_format_4, model.manifest.displayName))
            },
            text = {
                Text(
                    stringResource(CoreUiR.string.models_model_details_screen_43) +
                        "${model.totalBytes.toDetailsReadableBytes()}. Historical run metadata is preserved.",
                )
            },
            confirmButton = {
                Button(onClick = onConfirmDelete) {
                    Text(stringResource(CoreUiR.string.models_model_details_screen_44))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onCancelDelete) {
                    Text(stringResource(CoreUiR.string.models_model_details_screen_45))
                }
            },
        )
    }
    if (uiState.pendingHuggingFaceTokenModelId == modelId) {
        HuggingFaceTokenDialog(
            instruction = stringResource(CoreUiR.string.models_hugging_face_token_dialog_40),
            saveLabel = stringResource(CoreUiR.string.models_save_and_download),
            saving = uiState.isSavingHuggingFaceToken,
            error = uiState.huggingFaceTokenError,
            onSave = onSaveHuggingFaceToken,
            onDismiss = onDismissHuggingFaceToken,
        )
    }
}

@Composable
private fun ModelDetailsLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ModelUnavailable(message: String?, onNavigateBack: () -> Unit) {
    val dimensions = LocalAppDimensions.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensions.screenPadding)
            .padding(top = dimensions.topBarOverlayClearance + 20.dp),
        verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacing),
    ) {
        Text(
            text = stringResource(CoreUiR.string.models_model_details_screen_68),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(message ?: "This model is no longer present in the catalog or installed library.")
        Button(onClick = onNavigateBack) {
            Text(stringResource(CoreUiR.string.models_model_details_screen_69))
        }
    }
}
