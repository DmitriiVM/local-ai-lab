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
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.CatalogModel
import com.dmitriim.localaiplayground.core.model.InstalledModel
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelManifest
import com.dmitriim.localaiplayground.core.model.ModelTransferState
import com.dmitriim.localaiplayground.core.model.ModelValidationState
import com.dmitriim.localaiplayground.core.model.RuntimeProfileType
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions
import com.dmitriim.localaiplayground.core.result.StatusMessage
import com.dmitriim.localaiplayground.feature.models.presentation.ModelsUiState

@Composable
fun ModelsScreen(
    uiState: ModelsUiState,
    onDownload: (ModelId) -> Unit,
    onCancelTransfer: (ModelId) -> Unit,
    onDelete: (ModelId) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    var typeFilter by rememberSaveable { mutableStateOf(ModelTypeFilter.ALL) }
    var installationFilter by rememberSaveable { mutableStateOf(ModelInstallationFilter.ALL) }
    val modelItems = uiState.toModelListItems()
        .filter { typeFilter.matches(it.manifest) }
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
                installationFilter = installationFilter,
                onTypeFilterChange = { typeFilter = it },
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
                        onDelete = onDelete,
                    )
                    is ModelListItem.Catalog -> CatalogModelCard(
                        model = item.model,
                        transfer = uiState.transfers[item.manifest.modelId],
                        onDownload = onDownload,
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
    installationFilter: ModelInstallationFilter,
    onTypeFilterChange: (ModelTypeFilter) -> Unit,
    onInstallationFilterChange: (ModelInstallationFilter) -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Show models", style = MaterialTheme.typography.titleMedium)
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
            Text("Availability", style = MaterialTheme.typography.titleMedium)
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

private enum class ModelTypeFilter(
    val label: String,
    private val profileType: RuntimeProfileType? = null,
) {
    ALL(label = "All"),
    LLM(label = "LLM", profileType = RuntimeProfileType.LLM),
    TTS(label = "TTS", profileType = RuntimeProfileType.SUPERTONIC_TTS),
    STT(label = "STT", profileType = RuntimeProfileType.WHISPER_STT),
    VAD(label = "VAD", profileType = RuntimeProfileType.SILERO_VAD),
    ;

    fun matches(manifest: ModelManifest): Boolean = profileType == null || manifest.profileType == profileType
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
    onDelete: (ModelId) -> Unit,
) {
    Card {
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
    onDownload: (ModelId) -> Unit,
    onCancel: (ModelId) -> Unit,
) {
    val manifest = model.manifest
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ModelCardHeader(name = manifest.displayName, status = transfer.statusLabel())
            ModelCardMetadata(manifest = manifest, size = model.download.expectedBytes.toReadableBytes())
            when {
                transfer == ModelTransferState.Queued -> {
                    ModelCardAction { OutlinedButton(onClick = { onCancel(manifest.modelId) }) { Text("Cancel") } }
                }
                transfer is ModelTransferState.Running -> {
                    val total = transfer.totalBytes?.toReadableBytes() ?: "unknown size"
                    Text("${transfer.completedBytes.toReadableBytes()} / $total", style = MaterialTheme.typography.bodySmall)
                    ModelCardAction { OutlinedButton(onClick = { onCancel(manifest.modelId) }) { Text("Cancel") } }
                }
                transfer is ModelTransferState.Failed -> {
                    ModelCardAction { Button(onClick = { onDownload(manifest.modelId) }) { Text("Retry") } }
                }
                transfer == ModelTransferState.Cancelled -> {
                    ModelCardAction { Button(onClick = { onDownload(manifest.modelId) }) { Text("Download") } }
                }
                transfer == ModelTransferState.Idle || transfer == null ->
                    ModelCardAction { Button(onClick = { onDownload(manifest.modelId) }) { Text("Download") } }
            }
        }
    }
}

@Composable
private fun ModelCardHeader(name: String, status: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = name, style = MaterialTheme.typography.titleMedium)
        StatusBadge(status)
    }
}

@Composable
private fun ModelCardMetadata(manifest: ModelManifest, size: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TypeBadge(manifest.profileType.label)
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
        Text(status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
    }
}

private val RuntimeProfileType.label: String
    get() = when (this) {
        RuntimeProfileType.LLM -> "LLM"
        RuntimeProfileType.WHISPER_STT -> "STT"
        RuntimeProfileType.SILERO_VAD -> "VAD"
        RuntimeProfileType.SUPERTONIC_TTS -> "TTS"
    }

private fun ModelManifest.languageSummary(): String {
    val totalLanguageCount = supportedLanguageCount
    return when {
        profileType == RuntimeProfileType.SILERO_VAD -> "Language-independent"
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
    ModelTransferState.Queued -> "Queued"
    is ModelTransferState.Running -> "Downloading"
    ModelTransferState.Installing -> "Installing"
    is ModelTransferState.Failed -> "Download failed"
    ModelTransferState.Cancelled -> "Cancelled"
    ModelTransferState.Completed -> "Installed"
    ModelTransferState.Idle,
    null,
    -> "Not installed"
}

private fun Long.toReadableBytes(): String = when {
    this >= 1_073_741_824 -> "%.2f GiB".format(toDouble() / 1_073_741_824)
    this >= 1_048_576 -> "%.1f MiB".format(toDouble() / 1_048_576)
    else -> "$this B"
}
