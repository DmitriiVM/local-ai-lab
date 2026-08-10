package com.dmitriim.localaiplayground.feature.models.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.library.CatalogDownloadAuthentication
import com.dmitriim.localaiplayground.core.model.library.CatalogModel
import com.dmitriim.localaiplayground.core.model.library.InstalledModel
import com.dmitriim.localaiplayground.core.model.library.ModelTransferState
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.model.manifest.ModelManifest
import com.dmitriim.localaiplayground.core.result.StatusMessage
import com.dmitriim.localaiplayground.core.ui.layout.LocalAppDimensions
import com.dmitriim.localaiplayground.feature.models.presentation.ModelsUiState
import com.dmitriim.localaiplayground.core.ui.R as CoreUiR

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
    uiState.pendingDelete?.takeIf { it.manifest.modelId == modelId }?.let { model ->
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text(stringResource(CoreUiR.string.models_model_details_screen_format_4, model.manifest.displayName)) },
            text = { Text(stringResource(CoreUiR.string.models_model_details_screen_43) + "${model.totalBytes.toDetailsReadableBytes()}. Historical run metadata is preserved.") },
            confirmButton = { Button(onClick = onConfirmDelete) { Text(stringResource(CoreUiR.string.models_model_details_screen_44)) } },
            dismissButton = { OutlinedButton(onClick = onCancelDelete) { Text(stringResource(CoreUiR.string.models_model_details_screen_45)) } },
        )
    }
    if (uiState.pendingHuggingFaceTokenModelId == modelId) HuggingFaceTokenDialog(
        saving = uiState.isSavingHuggingFaceToken,
        error = uiState.huggingFaceTokenError,
        onSave = onSaveHuggingFaceToken,
        onDismiss = onDismissHuggingFaceToken,
    )
}

@Composable
private fun ModelDetailsContent(
    manifest: ModelManifest,
    catalogModel: CatalogModel?,
    installedModel: InstalledModel?,
    transfer: ModelTransferState?,
    uiState: ModelsUiState,
    onDownload: () -> Unit,
    onPauseTransfer: () -> Unit,
    onResumeOnWifi: () -> Unit,
    onResumeOnAnyNetwork: () -> Unit,
    onCancelTransfer: () -> Unit,
    onValidate: () -> Unit,
    onDelete: () -> Unit,
    onRequestHuggingFaceToken: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    val uriHandler = LocalUriHandler.current
    var technicalExpanded by rememberSaveable(manifest.modelId.value) { mutableStateOf(false) }
    val size = installedModel?.totalBytes ?: catalogModel?.download?.expectedBytes
    val status = installedModel?.validationState?.detailsStatusLabel() ?: transfer.detailsStatusLabel()
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        bottomBar = {
            ModelActionBar(installedModel, transfer, manifest.modelId in uiState.validatingModelIds, onDownload, onPauseTransfer, onResumeOnWifi, onResumeOnAnyNetwork, onCancelTransfer, onValidate, onDelete)
        },
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding).padding(horizontal = dimensions.screenPadding),
            contentPadding = PaddingValues(top = dimensions.topBarOverlayClearance + 40.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacing),
        ) {
            item { ModelDetailsHeader(manifest, status) }
            uiState.message?.let { message -> item { StatusMessage(stringResource(CoreUiR.string.models_lifecycle), message) } }
            item { DetailsSection("At a glance") { size?.let { DetailValue("Size", it.toDetailsReadableBytes()) }; DetailValue("Languages", manifest.detailsLanguageSummary()); manifest.approximateRamBytes?.let { DetailValue("Approximate RAM", it.toDetailsReadableBytes()) } } }
            item { DetailsSection("Model") { DetailValue("Family", manifest.family); DetailValue("Engine", manifest.engineId.value); DetailValue("Format", manifest.format.displayLabel()); manifest.architecture?.let { DetailValue("Architecture", it) }; manifest.quantization?.let { DetailValue("Quantization", it) } } }
            item { CapabilityDetails(manifest) }
            item { CompatibilityDetails(uiState, manifest.modelId) }
            item { InstallationDetails(installedModel, uiState.validationFeedback[manifest.modelId]) }
            item { ModelSourceDetails(manifest, uriHandler::openUri) }
            if (catalogModel?.download?.authentication == CatalogDownloadAuthentication.HUGGING_FACE_USER_TOKEN) item {
                HuggingFaceAccessSection(manifest.source.url, uiState.huggingFaceCredentialStatus, onRequestHuggingFaceToken)
            }
            item { TechnicalDetails(manifest, technicalExpanded) { technicalExpanded = !technicalExpanded } }
        }
    }
}

@Composable
private fun ModelDetailsHeader(manifest: ModelManifest, status: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(manifest.displayName, style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { DetailsBadge(manifest.detailsTypeLabel()); DetailsBadge(status) }
        Text(
            manifest.description ?: if (manifest.family == "Imported") "User-imported model." else "No description is available for this model.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ModelSourceDetails(manifest: ModelManifest, onOpenUrl: (String) -> Unit) {
    DetailsSection("Source and license") {
        DetailValue("License", manifest.source.licenseName)
        Text(manifest.source.attribution, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        manifest.source.url?.let { url -> OutlinedButton(onClick = { onOpenUrl(url) }) { Text(stringResource(CoreUiR.string.models_model_details_screen_46)) } }
    }
}

@Composable
private fun DetailsBadge(label: String) = androidx.compose.material3.Surface(
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
) { Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium) }

@Composable
private fun ModelDetailsLoading() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
private fun ModelUnavailable(message: String?, onNavigateBack: () -> Unit) {
    val dimensions = LocalAppDimensions.current
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = dimensions.screenPadding).padding(top = dimensions.topBarOverlayClearance + 20.dp),
        verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacing),
    ) {
        Text(stringResource(CoreUiR.string.models_model_details_screen_68), style = MaterialTheme.typography.headlineMedium)
        Text(message ?: "This model is no longer present in the catalog or installed library.")
        Button(onClick = onNavigateBack) { Text(stringResource(CoreUiR.string.models_model_details_screen_69)) }
    }
}

private fun Enum<*>.displayLabel(): String = name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
