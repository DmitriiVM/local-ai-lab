package com.dmitriim.localaiplayground.feature.models.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.CatalogModel
import com.dmitriim.localaiplayground.core.model.InstalledModel
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelTransferState
import com.dmitriim.localaiplayground.core.model.RuntimeProfileType
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions
import com.dmitriim.localaiplayground.core.result.StatusMessage
import com.dmitriim.localaiplayground.feature.models.presentation.ModelsUiState

@Composable
fun ModelsScreen(
    uiState: ModelsUiState,
    onImport: (RuntimeProfileType) -> Unit,
    onDownload: (ModelId) -> Unit,
    onCancelTransfer: (ModelId) -> Unit,
    onLoad: (ModelId) -> Unit,
    onUnload: (ModelId) -> Unit,
    onValidate: (ModelId) -> Unit,
    onDelete: (ModelId) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
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
        item {
            Text(
                "Models are copied into private app storage, verified, and never downloaded without confirmation.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        uiState.message?.let { message ->
            item { StatusMessage(title = "Model lifecycle", explanation = message) }
        }
        item {
            ImportCard(onImport = onImport)
        }
        if (uiState.installed.isEmpty()) {
            item {
                StatusMessage(
                    title = "No models installed",
                    explanation = "Import your own files or choose an approved curated model below.",
                )
            }
        } else {
            item { Text("Installed", style = MaterialTheme.typography.titleLarge) }
            items(uiState.installed.size, key = { "installed-${uiState.installed[it].manifest.modelId.value}" }) { index ->
                InstalledModelCard(
                    model = uiState.installed[index],
                    onLoad = onLoad,
                    onUnload = onUnload,
                    onValidate = onValidate,
                    onDelete = onDelete,
                )
            }
        }
        item { Text("Approved downloads", style = MaterialTheme.typography.titleLarge) }
        items(uiState.catalog.size, key = { "catalog-${uiState.catalog[it].manifest.modelId.value}" }) { index ->
            CatalogModelCard(
                model = uiState.catalog[index],
                transfer = uiState.transfers[uiState.catalog[index].manifest.modelId],
                onDownload = onDownload,
                onCancel = onCancelTransfer,
            )
        }
    }
    uiState.pendingDelete?.let { model ->
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text("Delete ${model.manifest.displayName}?") },
            text = { Text("This unloads the model and reclaims about ${model.totalBytes.toReadableBytes()}. Historical run metadata is preserved.") },
            confirmButton = { Button(onClick = onConfirmDelete) { Text("Delete") } },
            dismissButton = { OutlinedButton(onClick = onCancelDelete) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ImportCard(onImport: (RuntimeProfileType) -> Unit) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Import", style = MaterialTheme.typography.titleMedium)
            Text("Select every required companion file in the document picker. The app validates the selected runtime profile before installation.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onImport(RuntimeProfileType.LLM) }) { Text("GGUF") }
                OutlinedButton(onClick = { onImport(RuntimeProfileType.WHISPER_STT) }) { Text("Whisper") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onImport(RuntimeProfileType.SILERO_VAD) }) { Text("VAD") }
                OutlinedButton(onClick = { onImport(RuntimeProfileType.SUPERTONIC_TTS) }) { Text("Supertonic") }
            }
        }
    }
}

@Composable
private fun InstalledModelCard(
    model: InstalledModel,
    onLoad: (ModelId) -> Unit,
    onUnload: (ModelId) -> Unit,
    onValidate: (ModelId) -> Unit,
    onDelete: (ModelId) -> Unit,
) {
    val manifest = model.manifest
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(manifest.displayName, style = MaterialTheme.typography.titleMedium)
            Text("${manifest.family} • ${manifest.engineId.value} • ${manifest.profileType}")
            Text("${model.validationState} • ${model.totalBytes.toReadableBytes()}${if (model.loaded) " • Loaded" else ""}")
            model.validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text("${manifest.source.licenseName} • ${manifest.source.attribution}", style = MaterialTheme.typography.bodySmall)
            Text("Files: ${manifest.files.joinToString { it.relativePath }}", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (model.loaded) Button(onClick = { onUnload(manifest.modelId) }) { Text("Unload") }
                else Button(onClick = { onLoad(manifest.modelId) }) { Text("Load") }
                OutlinedButton(onClick = { onValidate(manifest.modelId) }) { Text("Validate") }
                OutlinedButton(onClick = { onDelete(manifest.modelId) }) { Text("Delete") }
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
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(model.manifest.displayName, style = MaterialTheme.typography.titleMedium)
            Text("${model.manifest.capabilities.joinToString()} • ${model.manifest.engineId.value}")
            Text("${model.download.expectedBytes.toReadableBytes()} • ${model.manifest.source.licenseName}")
            Text("SHA-256: ${model.download.sha256.take(16)}…", style = MaterialTheme.typography.bodySmall)
            when (transfer) {
                is ModelTransferState.Running -> {
                    val total = transfer.totalBytes?.toReadableBytes() ?: "unknown size"
                    Text("Downloading ${transfer.completedBytes.toReadableBytes()} / $total")
                    OutlinedButton(onClick = { onCancel(model.manifest.modelId) }) { Text("Cancel") }
                }
                is ModelTransferState.Failed -> Text(transfer.message, color = MaterialTheme.colorScheme.error)
                ModelTransferState.Cancelled -> Text("Download cancelled")
                ModelTransferState.Completed -> Text("Installed")
                ModelTransferState.Idle, null -> Button(onClick = { onDownload(model.manifest.modelId) }) { Text("Download") }
            }
        }
    }
}

private fun Long.toReadableBytes(): String = when {
    this >= 1_073_741_824 -> "%.2f GiB".format(toDouble() / 1_073_741_824)
    this >= 1_048_576 -> "%.1f MiB".format(toDouble() / 1_048_576)
    else -> "$this B"
}
