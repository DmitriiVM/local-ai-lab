package com.dmitriim.localaiplayground.feature.models.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import com.dmitriim.localaiplayground.core.model.ModelValidationState
import com.dmitriim.localaiplayground.core.model.RuntimeProfileType
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions
import com.dmitriim.localaiplayground.core.result.StatusMessage
import com.dmitriim.localaiplayground.feature.models.presentation.ModelValidationFeedback
import com.dmitriim.localaiplayground.feature.models.presentation.ModelsUiState

@Composable
fun ModelsScreen(
    uiState: ModelsUiState,
    onImport: (RuntimeProfileType) -> Unit,
    onDownload: (ModelId) -> Unit,
    onCancelTransfer: (ModelId) -> Unit,
    onValidate: (ModelId) -> Unit,
    onDelete: (ModelId) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    val installedModelIds = uiState.installed.mapTo(mutableSetOf()) { it.manifest.modelId }
    val approvedDownloads = uiState.catalog.filterNot { it.manifest.modelId in installedModelIds }
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
                    isValidating = uiState.installed[index].manifest.modelId in uiState.validatingModelIds,
                    validationFeedback = uiState.validationFeedback[uiState.installed[index].manifest.modelId],
                    onValidate = onValidate,
                    onDelete = onDelete,
                )
            }
        }
        if (approvedDownloads.isNotEmpty()) {
            item { Text("Approved downloads", style = MaterialTheme.typography.titleLarge) }
            items(approvedDownloads.size, key = { "catalog-${approvedDownloads[it].manifest.modelId.value}" }) { index ->
                CatalogModelCard(
                    model = approvedDownloads[index],
                    transfer = uiState.transfers[approvedDownloads[index].manifest.modelId],
                    onDownload = onDownload,
                    onCancel = onCancelTransfer,
                )
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
    isValidating: Boolean,
    validationFeedback: ModelValidationFeedback?,
    onValidate: (ModelId) -> Unit,
    onDelete: (ModelId) -> Unit,
) {
    val manifest = model.manifest
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(manifest.displayName, style = MaterialTheme.typography.titleMedium)
            Text("${manifest.family} • ${manifest.engineId.value} • ${manifest.profileType}")
            Text("${model.validationState} • ${model.totalBytes.toReadableBytes()}")
            model.validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text("${manifest.source.licenseName} • ${manifest.source.attribution}", style = MaterialTheme.typography.bodySmall)
            Text("Files: ${manifest.files.joinToString { it.relativePath }}", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onValidate(manifest.modelId) },
                    enabled = !isValidating,
                ) {
                    Text("Validate")
                }
                OutlinedButton(
                    onClick = { onDelete(manifest.modelId) },
                    enabled = !isValidating,
                ) {
                    Text("Delete")
                }
            }
            Box(Modifier.fillMaxWidth().heightIn(min = 20.dp)) {
                if (isValidating) {
                    Text(
                        text = "Validating model files…",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    validationFeedback?.let { feedback ->
                        Text(
                            text = feedback.message,
                            color = if (feedback.isError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
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
            if (model.download.files.isNotEmpty()) {
                Text(
                    "${model.download.files.size} individually verified files",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                model.download.sha256?.let { checksum ->
                    Text("SHA-256: ${checksum.take(16)}…", style = MaterialTheme.typography.bodySmall)
                }
            }
            when {
                transfer == ModelTransferState.Queued -> {
                    Text("Waiting to start download")
                    OutlinedButton(onClick = { onCancel(model.manifest.modelId) }) { Text("Cancel") }
                }
                transfer is ModelTransferState.Running -> {
                    val total = transfer.totalBytes?.toReadableBytes() ?: "unknown size"
                    Text("Downloading ${transfer.completedBytes.toReadableBytes()} / $total")
                    OutlinedButton(onClick = { onCancel(model.manifest.modelId) }) { Text("Cancel") }
                }
                transfer == ModelTransferState.Installing -> Text("Verifying and installing…")
                transfer is ModelTransferState.Failed -> {
                    Text(transfer.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { onDownload(model.manifest.modelId) }) { Text("Retry") }
                }
                transfer == ModelTransferState.Cancelled -> {
                    Text("Download cancelled")
                    Button(onClick = { onDownload(model.manifest.modelId) }) { Text("Download") }
                }
                transfer == ModelTransferState.Completed -> Text("Installed")
                transfer == ModelTransferState.Idle || transfer == null ->
                    Button(onClick = { onDownload(model.manifest.modelId) }) { Text("Download") }
            }
        }
    }
}

private fun Long.toReadableBytes(): String = when {
    this >= 1_073_741_824 -> "%.2f GiB".format(toDouble() / 1_073_741_824)
    this >= 1_048_576 -> "%.1f MiB".format(toDouble() / 1_048_576)
    else -> "$this B"
}
