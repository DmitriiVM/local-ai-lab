package com.dmitriim.localailab.feature.models.impl.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.library.CatalogDownloadAuthentication
import com.dmitriim.localailab.ai.api.model.library.CatalogModel
import com.dmitriim.localailab.feature.models.api.domain.library.InstalledModel
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferState
import com.dmitriim.localailab.feature.models.api.domain.library.ModelValidationState
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.feature.models.api.data.HuggingFaceCredentialStatus
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.component.StatusMessage
import com.dmitriim.localailab.core.ui.component.AppSurfaceCard
import com.dmitriim.localailab.core.ui.component.AppSurfaceTone
import com.dmitriim.localailab.core.ui.layout.LocalAppDimensions
import com.dmitriim.localailab.core.ui.style.AppFilterChipDefaults
import com.dmitriim.localailab.feature.models.impl.presentation.ModelsUiState

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

@Composable
private fun ModelsList(
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
    dimensions: com.dmitriim.localailab.core.ui.layout.AppDimensions,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensions.screenPadding),
        contentPadding = PaddingValues(bottom = dimensions.bottomNavigationOverlayClearance + 64.dp),
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
            item { Text(stringResource(CoreUiR.string.models_models_screen_format_10, modelItems.size), style = MaterialTheme.typography.titleLarge) }
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
private fun DeleteModelDialog(
    model: InstalledModel?,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
) {
    model?.let {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text(stringResource(CoreUiR.string.models_models_screen_format_11, it.manifest.displayName)) },
            text = { Text(stringResource(CoreUiR.string.models_models_screen_format_12, it.totalBytes.toReadableBytes())) },
            confirmButton = { Button(onClick = onConfirmDelete) { Text(stringResource(CoreUiR.string.models_models_screen_71)) } },
            dismissButton = { OutlinedButton(onClick = onCancelDelete) { Text(stringResource(CoreUiR.string.models_models_screen_72)) } },
        )
    }
}

@Composable
private fun ModelFilters(
    typeFilter: ModelTypeFilter,
    runtimeIds: List<EngineId>,
    runtimeFilter: String?,
    installationFilter: ModelInstallationFilter,
    onTypeFilterChange: (ModelTypeFilter) -> Unit,
    onRuntimeFilterChange: (String?) -> Unit,
    onInstallationFilterChange: (ModelInstallationFilter) -> Unit,
) {
    AppSurfaceCard(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ModelTypeFilter.entries.size) { index ->
                val filter = ModelTypeFilter.entries[index]
                FilterChip(
                    selected = typeFilter == filter,
                    onClick = { onTypeFilterChange(filter) },
                    label = { Text(stringResource(filter.labelRes)) },
                    colors = AppFilterChipDefaults.colors(),
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = runtimeFilter == null,
                    onClick = { onRuntimeFilterChange(null) },
                    label = { Text(stringResource(CoreUiR.string.models_models_screen_73)) },
                    colors = AppFilterChipDefaults.colors(),
                )
            }
            items(runtimeIds.size) { index ->
                val runtime = runtimeIds[index]
                FilterChip(
                    selected = runtimeFilter == runtime.value,
                    onClick = { onRuntimeFilterChange(runtime.value) },
                    label = { Text(runtime.displayLabel()) },
                    colors = AppFilterChipDefaults.colors(),
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ModelInstallationFilter.entries.size) { index ->
                val filter = ModelInstallationFilter.entries[index]
                FilterChip(
                    selected = installationFilter == filter,
                    onClick = { onInstallationFilterChange(filter) },
                    label = { Text(stringResource(filter.labelRes)) },
                    colors = AppFilterChipDefaults.colors(),
                )
            }
        }
    }
}

@Composable
private fun EngineId.displayLabel(): String = when (value) {
    "litert-lm" -> stringResource(CoreUiR.string.models_engine_litert_lm)
    else -> value
}

private enum class ModelTypeFilter(
    val labelRes: Int,
    private val capability: AiCapability? = null,
) {
    ALL(labelRes = CoreUiR.string.models_filter_all),
    LLM(labelRes = CoreUiR.string.models_type_llm, capability = AiCapability.CHAT),
    TTS(labelRes = CoreUiR.string.models_type_tts, capability = AiCapability.TEXT_TO_SPEECH),
    STT(labelRes = CoreUiR.string.models_type_stt, capability = AiCapability.SPEECH_TO_TEXT),
    ;

    fun matches(manifest: ModelManifest): Boolean = capability == null || capability in manifest.capabilities
}

private enum class ModelInstallationFilter(val labelRes: Int) {
    ALL(labelRes = CoreUiR.string.models_filter_all),
    INSTALLED(labelRes = CoreUiR.string.models_status_installed),
    NOT_INSTALLED(labelRes = CoreUiR.string.models_status_not_installed),
    ;

    fun matches(item: ModelListItem): Boolean = when (this) {
        ALL -> true
        INSTALLED -> item is ModelListItem.Installed
        NOT_INSTALLED -> item is ModelListItem.Catalog
    }
}

private sealed interface ModelListItem {
    val manifest: ModelManifest

    data class Installed(
        val model: InstalledModel,
        val catalogModel: CatalogModel? = null,
    ) : ModelListItem {
        override val manifest: ModelManifest = catalogModel?.manifest ?: model.manifest
    }

    data class Catalog(val model: CatalogModel) : ModelListItem {
        override val manifest: ModelManifest = model.manifest
    }
}

private fun ModelsUiState.toModelListItems(): List<ModelListItem> {
    val installedById = installed.associateBy { it.manifest.modelId }
    val catalogModelIds = catalog.mapTo(mutableSetOf()) { it.manifest.modelId }
    return buildList {
        catalog.forEach { catalogModel ->
            val installedModel = installedById[catalogModel.manifest.modelId]
            add(
                installedModel?.let { ModelListItem.Installed(it, catalogModel) }
                    ?: ModelListItem.Catalog(catalogModel),
            )
        }
        installed
            .filterNot { it.manifest.modelId in catalogModelIds }
            .forEach { add(ModelListItem.Installed(it)) }
    }.sortedWith(
        compareBy<ModelListItem> { it.manifest.displayName.lowercase() }
            .thenBy { it.manifest.modelId.value },
    )
}

@Composable
private fun InstalledModelCard(
    model: InstalledModel,
    displayManifest: ModelManifest,
    onOpenDetails: (ModelId) -> Unit,
    onDelete: (ModelId) -> Unit,
) {
    ModelCard(onClick = { onOpenDetails(model.manifest.modelId) }) {
        ModelCardIdentity(manifest = displayManifest, status = stringResource(model.validationState.statusLabelRes()))
        Spacer(modifier = Modifier.height(4.dp))
        ModelCardHeader(name = displayManifest.displayName)
        ModelCardMetadata(
            manifest = displayManifest,
            size = model.totalBytes.toReadableBytes(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = { onDelete(model.manifest.modelId) }) { Text(stringResource(CoreUiR.string.models_models_screen_74)) }
        }
    }
}

@Composable
private fun CatalogModelCard(
    model: CatalogModel,
    transfer: ModelTransferState?,
    huggingFaceCredentialStatus: com.dmitriim.localailab.feature.models.api.data.HuggingFaceCredentialStatus,
    onOpenDetails: (ModelId) -> Unit,
    onDownload: (ModelId) -> Unit,
    onPause: (ModelId) -> Unit,
    onResumeOnWifi: (ModelId) -> Unit,
    onCancel: (ModelId) -> Unit,
) {
    val manifest = model.manifest
    val accessRequired = model.download.authentication == CatalogDownloadAuthentication.HUGGING_FACE_USER_TOKEN &&
        huggingFaceCredentialStatus == com.dmitriim.localailab.feature.models.api.data.HuggingFaceCredentialStatus.MISSING
    var confirmCancel by rememberSaveable(manifest.modelId.value) { mutableStateOf(false) }
    ModelCard(onClick = { onOpenDetails(manifest.modelId) }) {
        ModelCardIdentity(manifest = manifest, status = stringResource(transfer.statusLabelRes()))
        Spacer(modifier = Modifier.height(4.dp))
        ModelCardHeader(name = manifest.displayName)
        ModelCardMetadata(
            manifest = manifest,
            size = model.download.expectedBytes.toReadableBytes(),
            downloadedBytes = transfer.downloadedBytesOrNull(),
        )
        if (accessRequired) {
            Text(stringResource(CoreUiR.string.models_models_screen_75), style = MaterialTheme.typography.bodySmall)
        }
        when {
            transfer is ModelTransferState.Queued -> {
                ModelCardAction {
                    OutlinedButton(onClick = { onPause(manifest.modelId) }) { Text(stringResource(CoreUiR.string.models_models_screen_76)) }
                    OutlinedButton(onClick = { confirmCancel = true }) { Text(stringResource(CoreUiR.string.models_models_screen_77)) }
                }
            }
            transfer is ModelTransferState.Running -> {
                ModelCardAction {
                    OutlinedButton(onClick = { onPause(manifest.modelId) }) { Text(stringResource(CoreUiR.string.models_models_screen_78)) }
                    OutlinedButton(onClick = { confirmCancel = true }) { Text(stringResource(CoreUiR.string.models_models_screen_79)) }
                }
            }
            transfer is ModelTransferState.Paused -> {
                ModelCardAction {
                    Button(onClick = { onResumeOnWifi(manifest.modelId) }) { Text(stringResource(CoreUiR.string.models_models_screen_80)) }
                    OutlinedButton(onClick = { confirmCancel = true }) { Text(stringResource(CoreUiR.string.models_models_screen_81)) }
                }
            }
            transfer == ModelTransferState.Installing -> {
                ModelCardAction {
                    OutlinedButton(onClick = {}, enabled = false) { Text(stringResource(CoreUiR.string.models_models_screen_82)) }
                }
            }
            transfer is ModelTransferState.Failed -> {
                ModelCardAction {
                    Button(onClick = { if (accessRequired) onOpenDetails(manifest.modelId) else onDownload(manifest.modelId) }) {
                        Text(
                            stringResource(
                                if (accessRequired) CoreUiR.string.models_set_up_access else CoreUiR.string.models_retry,
                            ),
                        )
                    }
                }
            }
            transfer == ModelTransferState.Idle || transfer == null -> ModelCardAction {
                if (accessRequired) {
                    Button(onClick = { onOpenDetails(manifest.modelId) }) {
                        Text(stringResource(CoreUiR.string.models_models_screen_83))
                    }
                } else {
                    ModelDownloadButton(onClick = { onDownload(manifest.modelId) })
                }
            }
        }
    }
    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text(stringResource(CoreUiR.string.models_models_screen_84)) },
            text = { Text(stringResource(CoreUiR.string.models_models_screen_85)) },
            confirmButton = {
                Button(onClick = {
                    confirmCancel = false
                    onCancel(manifest.modelId)
                }) { Text(stringResource(CoreUiR.string.models_models_screen_86)) }
            },
            dismissButton = { OutlinedButton(onClick = { confirmCancel = false }) { Text(stringResource(CoreUiR.string.models_models_screen_87)) } },
        )
    }
}

@Composable
private fun ModelCardIdentity(manifest: ModelManifest, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeBadge(stringResource(manifest.typeLabelRes()))
        Text(manifest.engineId.value, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.weight(1f))
        StatusBadge(status)
    }
}

@Composable
private fun ModelCardHeader(name: String) {
    Text(
        text = name,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun ModelCardMetadata(
    manifest: ModelManifest,
    size: String,
    downloadedBytes: Long? = null,
) {
    Text(
        text = buildAnnotatedString {
            downloadedBytes?.let {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.tertiary)) {
                    append(it.toReadableBytes())
                }
                append(" / ")
            }
            append(size)
            append(" • ${manifest.languageSummary()}")
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelCardAction(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
private fun TypeBadge(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun StatusBadge(status: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Clip,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun ModelCard(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    AppSurfaceCard(
        modifier = Modifier.clickable(onClick = onClick),
        tone = AppSurfaceTone.TONAL,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

private fun ModelManifest.typeLabelRes(): Int = when {
    AiCapability.CHAT in capabilities -> CoreUiR.string.models_type_llm
    AiCapability.TEXT_TO_SPEECH in capabilities -> CoreUiR.string.models_type_tts
    AiCapability.SPEECH_TO_TEXT in capabilities -> CoreUiR.string.models_type_stt
    AiCapability.VOICE_ACTIVITY_DETECTION in capabilities -> CoreUiR.string.models_type_vad
    else -> CoreUiR.string.models_type_model
}

@Composable
private fun ModelManifest.languageSummary(): String {
    val totalLanguageCount = supportedLanguageCount
    return when {
        AiCapability.VOICE_ACTIVITY_DETECTION in capabilities -> stringResource(CoreUiR.string.models_language_independent)
        languages.isEmpty() -> stringResource(CoreUiR.string.models_language_not_specified)
        totalLanguageCount != null && totalLanguageCount > languages.size ->
            "${languages.joinToString()} +${totalLanguageCount - languages.size}"
        else -> languages.joinToString()
    }
}

private fun ModelValidationState.statusLabelRes(): Int = when (this) {
    ModelValidationState.READY -> CoreUiR.string.models_status_ready
    ModelValidationState.INVALID,
    ModelValidationState.MISSING_FILES,
    ModelValidationState.INCOMPATIBLE,
    -> CoreUiR.string.models_status_needs_attention
}

private fun ModelTransferState?.statusLabelRes(): Int = when (this) {
    is ModelTransferState.Queued -> CoreUiR.string.models_status_queued
    is ModelTransferState.Running -> if (completedBytes >= totalBytes) CoreUiR.string.models_status_verifying else CoreUiR.string.models_status_downloading
    is ModelTransferState.Paused -> CoreUiR.string.models_status_paused
    ModelTransferState.Installing -> CoreUiR.string.models_status_installing
    is ModelTransferState.Failed -> CoreUiR.string.models_status_download_failed
    ModelTransferState.Completed -> CoreUiR.string.models_status_installed
    ModelTransferState.Idle,
    null,
    -> CoreUiR.string.models_status_not_installed
}

private fun ModelTransferState?.downloadedBytesOrNull(): Long? = when (this) {
    is ModelTransferState.Queued -> completedBytes
    is ModelTransferState.Running -> completedBytes
    is ModelTransferState.Paused -> completedBytes
    ModelTransferState.Completed,
    ModelTransferState.Idle,
    ModelTransferState.Installing,
    is ModelTransferState.Failed,
    null,
    -> null
}

private fun com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferNetworkPolicy.networkLabelRes(): Int = when (this) {
    com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferNetworkPolicy.WIFI_ONLY -> CoreUiR.string.models_network_wifi_only
    com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferNetworkPolicy.ANY_NETWORK -> CoreUiR.string.models_network_any
}

private fun Long.toReadableBytes(): String = when {
    this >= 1_073_741_824 -> "%.2f GiB".format(toDouble() / 1_073_741_824)
    this >= 1_048_576 -> "%.1f MiB".format(toDouble() / 1_048_576)
    else -> "$this B"
}
