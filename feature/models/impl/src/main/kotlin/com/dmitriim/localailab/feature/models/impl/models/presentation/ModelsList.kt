package com.dmitriim.localailab.feature.models.impl.models.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.component.StatusMessage
import com.dmitriim.localailab.core.ui.layout.AppDimensions
import com.dmitriim.localailab.feature.models.api.data.HuggingFaceCredentialStatus
import com.dmitriim.localailab.feature.models.api.domain.library.InstalledModel
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferState

@Composable
internal fun ModelsList(
    modelItems: List<ModelListItem>,
    runtimeIds: List<EngineId>,
    typeFilter: ModelTypeFilter,
    runtimeFilter: String?,
    installationFilter: ModelInstallationFilter,
    transfers: Map<ModelId, ModelTransferState>,
    credentialStatus: HuggingFaceCredentialStatus,
    onOpenDetails: (ModelId) -> Unit,
    onDownload: (ModelId) -> Unit,
    onPauseTransfer: (ModelId) -> Unit,
    onResumeOnWifi: (ModelId) -> Unit,
    onCancelTransfer: (ModelId) -> Unit,
    onDelete: (ModelId) -> Unit,
    onTypeFilterChange: (ModelTypeFilter) -> Unit,
    onRuntimeFilterChange: (String?) -> Unit,
    onInstallationFilterChange: (ModelInstallationFilter) -> Unit,
    dimensions: AppDimensions,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensions.screenPadding),
        contentPadding = PaddingValues(
            bottom = dimensions.bottomNavigationOverlayClearance + ModelDownloadActivityBarClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacing),
    ) {
        item {
            Text(
                text = stringResource(CoreUiR.string.models_title),
                modifier = Modifier
                    .padding(
                        top = dimensions.topBarOverlayClearance + 20.dp,
                        bottom = 20.dp,
                    ),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        item {
            ModelFilters(
                typeFilter = typeFilter,
                runtimeIds = runtimeIds,
                runtimeFilter = runtimeFilter,
                installationFilter = installationFilter,
                onTypeFilterChange = onTypeFilterChange,
                onRuntimeFilterChange = onRuntimeFilterChange,
                onInstallationFilterChange = onInstallationFilterChange,
            )
        }
        if (modelItems.isEmpty()) {
            item {
                StatusMessage(
                    title = stringResource(CoreUiR.string.models_no_matching),
                    explanation = stringResource(CoreUiR.string.models_no_matching_explanation),
                )
            }
        } else {
            item {
                Text(
                    text = stringResource(
                        CoreUiR.string.models_models_screen_format_10,
                        modelItems.size,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            items(modelItems.size, key = { modelItems[it].manifest.modelId.value }) { index ->
                when (val item = modelItems[index]) {
                    is ModelListItem.Installed -> InstalledModelCard(
                        model = item.model,
                        displayManifest = item.manifest,
                        onOpenDetails = onOpenDetails,
                        onDelete = onDelete,
                    )
                    is ModelListItem.Catalog -> CatalogModelCard(
                        model = item.model,
                        transfer = transfers[item.manifest.modelId],
                        huggingFaceCredentialStatus = credentialStatus,
                        onOpenDetails = onOpenDetails,
                        onDownload = onDownload,
                        onPause = onPauseTransfer,
                        onResumeOnWifi = onResumeOnWifi,
                        onCancel = onCancelTransfer,
                    )
                }
            }
        }
    }
}

@Composable
internal fun DeleteModelDialog(
    model: InstalledModel?,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
) {
    model?.let {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = {
                Text(
                    stringResource(
                        CoreUiR.string.models_models_screen_format_11,
                        it.manifest.displayName,
                    ),
                )
            },
            text = {
                Text(
                    stringResource(
                        CoreUiR.string.models_models_screen_format_12,
                        it.totalBytes.toReadableBytes(),
                    ),
                )
            },
            confirmButton = {
                Button(onClick = onConfirmDelete) {
                    Text(stringResource(CoreUiR.string.models_models_screen_71))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onCancelDelete) {
                    Text(stringResource(CoreUiR.string.models_models_screen_72))
                }
            },
        )
    }
}
