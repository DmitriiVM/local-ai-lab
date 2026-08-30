package com.dmitriim.localailab.feature.models.impl.models.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.dmitriim.localailab.ai.api.model.library.CatalogDownloadAuthentication
import com.dmitriim.localailab.ai.api.model.library.CatalogModel
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.component.AppSurfaceCard
import com.dmitriim.localailab.core.ui.component.AppSurfaceTone
import com.dmitriim.localailab.feature.models.api.data.HuggingFaceCredentialStatus
import com.dmitriim.localailab.feature.models.api.domain.library.InstalledModel
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferState

@Composable
internal fun InstalledModelCard(
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
            OutlinedButton(onClick = { onDelete(model.manifest.modelId) }) {
                Text(stringResource(CoreUiR.string.models_models_screen_74))
            }
        }
    }
}

@Composable
internal fun CatalogModelCard(
    model: CatalogModel,
    transfer: ModelTransferState?,
    huggingFaceCredentialStatus: HuggingFaceCredentialStatus,
    onOpenDetails: (ModelId) -> Unit,
    onDownload: (ModelId) -> Unit,
    onPause: (ModelId) -> Unit,
    onResumeOnWifi: (ModelId) -> Unit,
    onCancel: (ModelId) -> Unit,
) {
    val manifest = model.manifest
    val accessRequired = model.download.authentication == CatalogDownloadAuthentication.HUGGING_FACE_USER_TOKEN &&
        huggingFaceCredentialStatus == HuggingFaceCredentialStatus.MISSING
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
        CatalogModelCardActions(
            transfer = transfer,
            modelId = manifest.modelId,
            accessRequired = accessRequired,
            onOpenDetails = onOpenDetails,
            onDownload = onDownload,
            onPause = onPause,
            onResumeOnWifi = onResumeOnWifi,
            onCancel = { confirmCancel = true },
        )
    }
    CatalogCancelDialog(
        visible = confirmCancel,
        onDismiss = { confirmCancel = false },
        onConfirm = {
            confirmCancel = false
            onCancel(manifest.modelId)
        },
    )
}

@Composable
private fun CatalogModelCardActions(
    transfer: ModelTransferState?,
    modelId: ModelId,
    accessRequired: Boolean,
    onOpenDetails: (ModelId) -> Unit,
    onDownload: (ModelId) -> Unit,
    onPause: (ModelId) -> Unit,
    onResumeOnWifi: (ModelId) -> Unit,
    onCancel: () -> Unit,
) {
    when {
        transfer is ModelTransferState.Queued -> ModelCardAction {
            OutlinedButton(onClick = { onPause(modelId) }) {
                Text(stringResource(CoreUiR.string.models_models_screen_76))
            }
            OutlinedButton(onClick = onCancel) {
                Text(stringResource(CoreUiR.string.models_models_screen_77))
            }
        }
        transfer is ModelTransferState.Running -> ModelCardAction {
            OutlinedButton(onClick = { onPause(modelId) }) {
                Text(stringResource(CoreUiR.string.models_models_screen_78))
            }
            OutlinedButton(onClick = onCancel) {
                Text(stringResource(CoreUiR.string.models_models_screen_79))
            }
        }
        transfer is ModelTransferState.Paused -> ModelCardAction {
            Button(onClick = { onResumeOnWifi(modelId) }) {
                Text(stringResource(CoreUiR.string.models_models_screen_80))
            }
            OutlinedButton(onClick = onCancel) {
                Text(stringResource(CoreUiR.string.models_models_screen_81))
            }
        }
        transfer == ModelTransferState.Installing -> ModelCardAction {
            OutlinedButton(onClick = {}, enabled = false) {
                Text(stringResource(CoreUiR.string.models_models_screen_82))
            }
        }
        transfer is ModelTransferState.Failed -> ModelCardAction {
            Button(onClick = {
                if (accessRequired) {
                    onOpenDetails(modelId)
                } else {
                    onDownload(modelId)
                }
            }) {
                Text(
                    stringResource(
                        if (accessRequired) CoreUiR.string.models_set_up_access else CoreUiR.string.models_retry,
                    ),
                )
            }
        }
        transfer == ModelTransferState.Idle || transfer == null -> ModelCardAction {
            if (accessRequired) {
                Button(onClick = { onOpenDetails(modelId) }) {
                    Text(stringResource(CoreUiR.string.models_models_screen_83))
                }
            } else {
                ModelDownloadButton(onClick = { onDownload(modelId) })
            }
        }
    }
}

@Composable
private fun CatalogCancelDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(CoreUiR.string.models_models_screen_84)) },
            text = { Text(stringResource(CoreUiR.string.models_models_screen_85)) },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text(stringResource(CoreUiR.string.models_models_screen_86))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(CoreUiR.string.models_models_screen_87))
                }
            },
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
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
        )
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
