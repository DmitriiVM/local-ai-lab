package com.dmitriim.localailab.feature.models.impl.details.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.model.library.CatalogDownloadAuthentication
import com.dmitriim.localailab.ai.api.model.library.CatalogModel
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.component.StatusMessage
import com.dmitriim.localailab.core.ui.layout.AppDimensions
import com.dmitriim.localailab.core.ui.layout.LocalAppDimensions
import com.dmitriim.localailab.feature.models.api.domain.library.InstalledModel
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferState
import com.dmitriim.localailab.feature.models.impl.models.presentation.ModelsUiState

@Composable
internal fun ModelDetailsContent(
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
    val status = installedModel?.validationState?.detailsStatusLabel() ?: transfer.detailsStatusLabel()
    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        bottomBar = {
            ModelActionBar(
                installedModel = installedModel,
                transfer = transfer,
                validating = manifest.modelId in uiState.validatingModelIds,
                onDownload = onDownload,
                onPauseTransfer = onPauseTransfer,
                onResumeOnWifi = onResumeOnWifi,
                onResumeOnAnyNetwork = onResumeOnAnyNetwork,
                onCancelTransfer = onCancelTransfer,
                onValidate = onValidate,
                onDelete = onDelete,
            )
        },
    ) { scaffoldPadding ->
        ModelDetailsList(
            manifest = manifest,
            catalogModel = catalogModel,
            installedModel = installedModel,
            uiState = uiState,
            status = status,
            dimensions = dimensions,
            scaffoldPadding = scaffoldPadding,
            onOpenUrl = uriHandler::openUri,
            technicalExpanded = technicalExpanded,
            onToggleTechnicalDetails = { technicalExpanded = !technicalExpanded },
            onRequestHuggingFaceToken = onRequestHuggingFaceToken,
        )
    }
}

@Composable
private fun ModelDetailsList(
    manifest: ModelManifest,
    catalogModel: CatalogModel?,
    installedModel: InstalledModel?,
    uiState: ModelsUiState,
    status: String,
    dimensions: AppDimensions,
    scaffoldPadding: PaddingValues,
    onOpenUrl: (String) -> Unit,
    technicalExpanded: Boolean,
    onToggleTechnicalDetails: () -> Unit,
    onRequestHuggingFaceToken: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
            .padding(horizontal = dimensions.screenPadding),
        contentPadding = PaddingValues(top = dimensions.topBarOverlayClearance + 40.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacing),
    ) {
        modelDetailsItems(
            manifest = manifest,
            catalogModel = catalogModel,
            installedModel = installedModel,
            uiState = uiState,
            status = status,
            onOpenUrl = onOpenUrl,
            technicalExpanded = technicalExpanded,
            onToggleTechnicalDetails = onToggleTechnicalDetails,
            onRequestHuggingFaceToken = onRequestHuggingFaceToken,
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.modelDetailsItems(
    manifest: ModelManifest,
    catalogModel: CatalogModel?,
    installedModel: InstalledModel?,
    uiState: ModelsUiState,
    status: String,
    onOpenUrl: (String) -> Unit,
    technicalExpanded: Boolean,
    onToggleTechnicalDetails: () -> Unit,
    onRequestHuggingFaceToken: () -> Unit,
) {
    item { ModelDetailsHeader(manifest, status) }
    uiState.message?.let { message ->
        item { StatusMessage(stringResource(CoreUiR.string.models_lifecycle), message) }
    }
    item {
        ModelOverviewSection(
            manifest = manifest,
            size = installedModel?.totalBytes ?: catalogModel?.download?.expectedBytes,
        )
    }
    catalogModel?.let { model -> item { DownloadStorageDetails(model = model) } }
    item { ModelMetadataSection(manifest = manifest) }
    item { CapabilityDetails(manifest = manifest) }
    item { CompatibilityDetails(uiState = uiState, modelId = manifest.modelId) }
    item {
        InstallationDetails(
            model = installedModel,
            validationFeedback = uiState.validationFeedback[manifest.modelId],
        )
    }
    item { ModelSourceDetails(manifest, onOpenUrl) }
    if (catalogModel?.download?.authentication == CatalogDownloadAuthentication.HUGGING_FACE_USER_TOKEN) {
        item {
            HuggingFaceAccessSection(
                accessUrl = manifest.source.url,
                credentialStatus = uiState.huggingFaceCredentialStatus,
                onConfigure = onRequestHuggingFaceToken,
            )
        }
    }
    item {
        TechnicalDetails(
            manifest = manifest,
            expanded = technicalExpanded,
            onToggle = onToggleTechnicalDetails,
        )
    }
}
