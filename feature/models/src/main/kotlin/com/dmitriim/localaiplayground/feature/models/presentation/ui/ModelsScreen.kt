package com.dmitriim.localaiplayground.feature.models.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.library.CatalogModel
import com.dmitriim.localaiplayground.core.model.library.CatalogDownloadAuthentication
import com.dmitriim.localaiplayground.core.model.library.InstalledModel
import com.dmitriim.localaiplayground.core.model.library.ModelTransferState
import com.dmitriim.localaiplayground.core.model.library.ModelValidationState
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.model.manifest.ModelManifest
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions
import com.dmitriim.localaiplayground.core.result.StatusMessage
import com.dmitriim.localaiplayground.feature.models.presentation.ModelsUiState

@Composable
fun ModelsScreen(
    uiState: ModelsUiState,
    onOpenDetails: (ModelId) -> Unit,
    onDownload: (ModelId) -> Unit,
    onPauseTransfer: (ModelId) -> Unit,
    onResumeOnWifi: (ModelId) -> Unit,
    onResumeOnAnyNetwork: (ModelId) -> Unit,
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
        .sortedBy { it.displayLabel }
    val modelItems = allModelItems
        .filter { typeFilter.matches(it.manifest) }
        .filter { runtimeFilter == null || it.manifest.engineId.value == runtimeFilter }
        .filter { installationFilter.matches(it) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = dimensions.bottomNavigationOverlayClearance + 64.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Models",
                modifier = Modifier.padding(top = dimensions.topBarOverlayClearance + 20.dp),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        uiState.message?.let { message ->
            item { StatusMessage(title = "Model lifecycle", explanation = message) }
        }
        item {
            ModelFilters(
                typeFilter = typeFilter,
                runtimeIds = runtimeIds,
                runtimeFilter = runtimeFilter,
                installationFilter = installationFilter,
                onTypeFilterChange = { typeFilter = it },
                onRuntimeFilterChange = { runtimeFilter = it },
                onInstallationFilterChange = { installationFilter = it },
            )
        }
        if (modelItems.isEmpty()) {
            item {
                StatusMessage(
                    title = "No matching models",
                    explanation = "Try a different model type or availability filter.",
                )
            }
        } else {
            item { Text("Models (${modelItems.size})", style = MaterialTheme.typography.titleLarge) }
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
                        transfer = uiState.transfers[item.manifest.modelId],
                        huggingFaceCredentialStatus = uiState.huggingFaceCredentialStatus,
                        onOpenDetails = onOpenDetails,
                        onDownload = onDownload,
                        onPause = onPauseTransfer,
                        onResumeOnWifi = onResumeOnWifi,
                        onResumeOnAnyNetwork = onResumeOnAnyNetwork,
                        onCancel = onCancelTransfer,
                    )
                }
            }
        }
    }
    uiState.pendingDelete?.let { model ->
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text("Delete ${model.manifest.displayName}?") },
            text = { Text("This deletes the model files and reclaims about ${model.totalBytes.toReadableBytes()}. Historical run metadata is preserved.") },
            confirmButton = { Button(onClick = onConfirmDelete) { Text("Delete") } },
            dismissButton = { OutlinedButton(onClick = onCancelDelete) { Text("Cancel") } },
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
    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ModelTypeFilter.entries.size) { index ->
                    val filter = ModelTypeFilter.entries[index]
                    FilterChip(
                        selected = typeFilter == filter,
                        onClick = { onTypeFilterChange(filter) },
                        label = { Text(filter.label) },
                    )
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = runtimeFilter == null,
                        onClick = { onRuntimeFilterChange(null) },
                        label = { Text("All") },
                    )
                }
                items(runtimeIds.size) { index ->
                    val runtime = runtimeIds[index]
                    FilterChip(
                        selected = runtimeFilter == runtime.value,
                        onClick = { onRuntimeFilterChange(runtime.value) },
                        label = { Text(runtime.displayLabel) },
                    )
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ModelInstallationFilter.entries.size) { index ->
                    val filter = ModelInstallationFilter.entries[index]
                    FilterChip(
                        selected = installationFilter == filter,
                        onClick = { onInstallationFilterChange(filter) },
                        label = { Text(filter.label) },
                    )
                }
            }
        }
    }
}

private val EngineId.displayLabel: String
    get() = when (value) {
        "litert-lm" -> "LiteRT-LM"
        else -> value
    }

private enum class ModelTypeFilter(
    val label: String,
    private val capability: AiCapability? = null,
) {
    ALL(label = "All"),
    LLM(label = "LLM", capability = AiCapability.CHAT),
    TTS(label = "TTS", capability = AiCapability.TEXT_TO_SPEECH),
    STT(label = "STT", capability = AiCapability.SPEECH_TO_TEXT),
    ;

    fun matches(manifest: ModelManifest): Boolean = capability == null || capability in manifest.capabilities
}

private enum class ModelInstallationFilter(val label: String) {
    ALL(label = "All"),
    INSTALLED(label = "Installed"),
    NOT_INSTALLED(label = "Not installed"),
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
    }
}

@Composable
private fun InstalledModelCard(
    model: InstalledModel,
    displayManifest: ModelManifest,
    onOpenDetails: (ModelId) -> Unit,
    onDelete: (ModelId) -> Unit,
) {
    Card(onClick = { onOpenDetails(model.manifest.modelId) }) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ModelCardHeader(
                name = displayManifest.displayName,
                status = model.validationState.statusLabel(),
            )
            ModelCardMetadata(
                manifest = displayManifest,
                size = model.totalBytes.toReadableBytes(),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = { onDelete(model.manifest.modelId) }) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun CatalogModelCard(
    model: CatalogModel,
    transfer: ModelTransferState?,
    huggingFaceCredentialStatus: com.dmitriim.localaiplayground.core.model.service.HuggingFaceCredentialStatus,
    onOpenDetails: (ModelId) -> Unit,
    onDownload: (ModelId) -> Unit,
    onPause: (ModelId) -> Unit,
    onResumeOnWifi: (ModelId) -> Unit,
    onResumeOnAnyNetwork: (ModelId) -> Unit,
    onCancel: (ModelId) -> Unit,
) {
    val manifest = model.manifest
    val accessRequired = model.download.authentication == CatalogDownloadAuthentication.HUGGING_FACE_USER_TOKEN &&
        huggingFaceCredentialStatus == com.dmitriim.localaiplayground.core.model.service.HuggingFaceCredentialStatus.MISSING
    var confirmCancel by rememberSaveable(manifest.modelId.value) { mutableStateOf(false) }
    var confirmAnyNetwork by rememberSaveable(manifest.modelId.value) { mutableStateOf(false) }
    Card(onClick = { onOpenDetails(manifest.modelId) }) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ModelCardHeader(name = manifest.displayName, status = transfer.statusLabel())
            ModelCardMetadata(manifest = manifest, size = model.download.expectedBytes.toReadableBytes())
            if (accessRequired) {
                Text("Hugging Face access required", style = MaterialTheme.typography.bodySmall)
            }
            when {
                transfer is ModelTransferState.Queued -> {
                    Text("${transfer.networkPolicy.networkLabel()}", style = MaterialTheme.typography.bodySmall)
                    ModelCardAction {
                        OutlinedButton(onClick = { onPause(manifest.modelId) }) { Text("Pause") }
                        if (transfer.networkPolicy == com.dmitriim.localaiplayground.core.model.library.ModelTransferNetworkPolicy.WIFI_ONLY) {
                            OutlinedButton(onClick = { confirmAnyNetwork = true }) { Text("Use mobile data") }
                        }
                        OutlinedButton(onClick = { confirmCancel = true }) { Text("Cancel") }
                    }
                }
                transfer is ModelTransferState.Running -> {
                    val total = transfer.totalBytes.toReadableBytes()
                    Text("${transfer.completedBytes.toReadableBytes()} / $total", style = MaterialTheme.typography.bodySmall)
                    ModelCardAction {
                        OutlinedButton(onClick = { onPause(manifest.modelId) }) { Text("Pause") }
                        OutlinedButton(onClick = { confirmCancel = true }) { Text("Cancel") }
                    }
                }
                transfer is ModelTransferState.Paused -> {
                    Text("${transfer.completedBytes.toReadableBytes()} / ${transfer.totalBytes.toReadableBytes()}", style = MaterialTheme.typography.bodySmall)
                    transfer.reason?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    ModelCardAction {
                        Button(onClick = { onResumeOnWifi(manifest.modelId) }) { Text("Resume") }
                        OutlinedButton(onClick = { confirmAnyNetwork = true }) { Text("Use mobile data") }
                        OutlinedButton(onClick = { confirmCancel = true }) { Text("Cancel") }
                    }
                }
                transfer is ModelTransferState.Failed -> {
                    ModelCardAction {
                        Button(onClick = { if (accessRequired) onOpenDetails(manifest.modelId) else onDownload(manifest.modelId) }) {
                            Text(if (accessRequired) "Set up access" else "Retry")
                        }
                    }
                }
                transfer == ModelTransferState.Idle || transfer == null ->
                    ModelCardAction {
                        Button(onClick = { if (accessRequired) onOpenDetails(manifest.modelId) else onDownload(manifest.modelId) }) {
                            Text(if (accessRequired) "Set up access" else "Download")
                        }
                    }
            }
        }
    }
    if (confirmAnyNetwork) {
        AlertDialog(
            onDismissRequest = { confirmAnyNetwork = false },
            title = { Text("Use mobile data?") },
            text = { Text("This model can be large. This transfer may use your mobile-data allowance.") },
            confirmButton = {
                Button(onClick = {
                    confirmAnyNetwork = false
                    onResumeOnAnyNetwork(manifest.modelId)
                }) { Text("Use mobile data") }
            },
            dismissButton = { OutlinedButton(onClick = { confirmAnyNetwork = false }) { Text("Keep Wi-Fi only") } },
        )
    }
    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Cancel download?") },
            text = { Text("The partial model download will be permanently deleted.") },
            confirmButton = {
                Button(onClick = {
                    confirmCancel = false
                    onCancel(manifest.modelId)
                }) { Text("Cancel download") }
            },
            dismissButton = { OutlinedButton(onClick = { confirmCancel = false }) { Text("Keep download") } },
        )
    }
}

@Composable
private fun ModelCardHeader(name: String, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
        )
        StatusBadge(status)
    }
}

@Composable
private fun ModelCardMetadata(manifest: ModelManifest, size: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TypeBadge(manifest.typeLabel)
        Text(manifest.engineId.value, style = MaterialTheme.typography.bodyMedium)
    }
    Text(
        "$size • ${manifest.languageSummary()}",
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun ModelCardAction(content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
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

private val ModelManifest.typeLabel: String
    get() = when {
        AiCapability.CHAT in capabilities -> "LLM"
        AiCapability.TEXT_TO_SPEECH in capabilities -> "TTS"
        AiCapability.SPEECH_TO_TEXT in capabilities -> "STT"
        AiCapability.VOICE_ACTIVITY_DETECTION in capabilities -> "VAD"
        else -> "Model"
    }

private fun ModelManifest.languageSummary(): String {
    val totalLanguageCount = supportedLanguageCount
    return when {
        AiCapability.VOICE_ACTIVITY_DETECTION in capabilities -> "Language-independent"
        languages.isEmpty() -> "Language not specified"
        totalLanguageCount != null && totalLanguageCount > languages.size ->
            "${languages.joinToString()} +${totalLanguageCount - languages.size}"
        else -> languages.joinToString()
    }
}

private fun ModelValidationState.statusLabel(): String = when (this) {
    ModelValidationState.READY -> "Ready"
    ModelValidationState.INVALID,
    ModelValidationState.MISSING_FILES,
    ModelValidationState.INCOMPATIBLE,
    -> "Needs attention"
}

private fun ModelTransferState?.statusLabel(): String = when (this) {
    is ModelTransferState.Queued -> "Queued"
    is ModelTransferState.Running -> "Downloading"
    is ModelTransferState.Paused -> "Paused"
    ModelTransferState.Installing -> "Installing"
    is ModelTransferState.Failed -> "Download failed"
    ModelTransferState.Completed -> "Installed"
    ModelTransferState.Idle,
    null,
    -> "Not installed"
}

private fun com.dmitriim.localaiplayground.core.model.library.ModelTransferNetworkPolicy.networkLabel(): String = when (this) {
    com.dmitriim.localaiplayground.core.model.library.ModelTransferNetworkPolicy.WIFI_ONLY -> "Wi-Fi only"
    com.dmitriim.localaiplayground.core.model.library.ModelTransferNetworkPolicy.ANY_NETWORK -> "Any network"
}

private fun Long.toReadableBytes(): String = when {
    this >= 1_073_741_824 -> "%.2f GiB".format(toDouble() / 1_073_741_824)
    this >= 1_048_576 -> "%.1f MiB".format(toDouble() / 1_048_576)
    else -> "$this B"
}
