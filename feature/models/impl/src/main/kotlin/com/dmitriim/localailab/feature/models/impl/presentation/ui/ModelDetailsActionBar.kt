package com.dmitriim.localailab.feature.models.impl.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.feature.models.api.domain.library.InstalledModel
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferNetworkPolicy
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferState

@Composable
internal fun ModelActionBar(
    installedModel: InstalledModel?,
    transfer: ModelTransferState?,
    validating: Boolean,
    onDownload: () -> Unit,
    onPauseTransfer: () -> Unit,
    onResumeOnWifi: () -> Unit,
    onResumeOnAnyNetwork: () -> Unit,
    onCancelTransfer: () -> Unit,
    onValidate: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmCancel by rememberSaveable { mutableStateOf(false) }
    var confirmAnyNetwork by rememberSaveable { mutableStateOf(false) }
    Surface(shadowElevation = 8.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (installedModel != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onValidate,
                        enabled = !validating,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.tertiary.copy(alpha = 0.58f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.tertiary),
                    ) { Text(stringResource(if (validating) CoreUiR.string.models_validating else CoreUiR.string.models_validate)) }
                    OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.error)) {
                        Text(stringResource(CoreUiR.string.models_model_details_screen_50))
                    }
                }
            } else {
                when (transfer) {
                    is ModelTransferState.Queued -> TransferQueuedActions(transfer, onPauseTransfer, { confirmCancel = true }, { confirmAnyNetwork = true })
                    is ModelTransferState.Running -> TransferRunningActions(transfer, onPauseTransfer, { confirmCancel = true })
                    is ModelTransferState.Paused -> TransferPausedActions(transfer, onResumeOnWifi, { confirmCancel = true })
                    ModelTransferState.Installing, ModelTransferState.Completed -> Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text(stringResource(CoreUiR.string.models_model_details_screen_58)) }
                    is ModelTransferState.Failed -> {
                        Text(transfer.message, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                        Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) { Text(stringResource(CoreUiR.string.models_model_details_screen_59)) }
                    }
                    ModelTransferState.Idle, null -> ModelDownloadButton(onClick = onDownload, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
    if (confirmAnyNetwork) {
        AlertDialog(
            onDismissRequest = { confirmAnyNetwork = false },
            title = { Text(stringResource(CoreUiR.string.models_model_details_screen_60)) },
            text = { Text(stringResource(CoreUiR.string.models_model_details_screen_61)) },
            confirmButton = {
                Button(onClick = {
                    confirmAnyNetwork = false
                    onResumeOnAnyNetwork()
                }) { Text(stringResource(CoreUiR.string.models_model_details_screen_62)) }
            },
            dismissButton = { OutlinedButton(onClick = { confirmAnyNetwork = false }) { Text(stringResource(CoreUiR.string.models_model_details_screen_63)) } },
        )
    }
    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text(stringResource(CoreUiR.string.models_model_details_screen_64)) },
            text = { Text(stringResource(CoreUiR.string.models_model_details_screen_65)) },
            confirmButton = {
                Button(onClick = {
                    confirmCancel = false
                    onCancelTransfer()
                }) { Text(stringResource(CoreUiR.string.models_model_details_screen_66)) }
            },
            dismissButton = { OutlinedButton(onClick = { confirmCancel = false }) { Text(stringResource(CoreUiR.string.models_model_details_screen_67)) } },
        )
    }
}

@Composable
private fun TransferQueuedActions(transfer: ModelTransferState.Queued, onPause: () -> Unit, onCancel: () -> Unit, onUseAnyNetwork: () -> Unit) {
    Text(stringResource(CoreUiR.string.models_model_details_screen_format_7, transfer.networkPolicy.detailsNetworkLabel()))
    Text(
        stringResource(CoreUiR.string.models_model_download_battery_requirement),
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f)) { Text(stringResource(CoreUiR.string.models_model_details_screen_51)) }
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(CoreUiR.string.models_model_details_screen_52)) }
    }
    if (transfer.networkPolicy == ModelTransferNetworkPolicy.WIFI_ONLY) OutlinedButton(onClick = onUseAnyNetwork, modifier = Modifier.fillMaxWidth()) { Text(stringResource(CoreUiR.string.models_model_details_screen_53)) }
}

@Composable
private fun TransferRunningActions(transfer: ModelTransferState.Running, onPause: () -> Unit, onCancel: () -> Unit) {
    Text(stringResource(CoreUiR.string.models_model_details_screen_format_8, transfer.completedBytes.toDetailsReadableBytes(), transfer.totalBytes.toDetailsReadableBytes()))
    transfer.bytesPerSecond?.takeIf { it > 0L }?.let { bytesPerSecond ->
        val remaining = transfer.estimatedRemainingMillis?.toDetailsRemainingDuration() ?: return@let
        Text(
            stringResource(
                CoreUiR.string.models_model_transfer_speed_eta,
                bytesPerSecond.toDetailsReadableBytes(),
                remaining,
            ),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f)) { Text(stringResource(CoreUiR.string.models_model_details_screen_54)) }
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(CoreUiR.string.models_model_details_screen_55)) }
    }
}

private fun Long.toDetailsRemainingDuration(): String {
    val remainingSeconds = (coerceAtLeast(0L) + 999L) / 1_000L
    return when {
        remainingSeconds < 60L -> "< 1 min"
        remainingSeconds < 3_600L -> "${(remainingSeconds + 59L) / 60L} min"
        else -> {
            val hours = remainingSeconds / 3_600L
            val minutes = (remainingSeconds % 3_600L + 59L) / 60L
            if (minutes == 60L) "${hours + 1L} h" else "$hours h $minutes min"
        }
    }
}

@Composable
private fun TransferPausedActions(transfer: ModelTransferState.Paused, onResume: () -> Unit, onCancel: () -> Unit) {
    Text(stringResource(CoreUiR.string.models_model_details_screen_format_9, transfer.completedBytes.toDetailsReadableBytes(), transfer.totalBytes.toDetailsReadableBytes()))
    transfer.reason?.let { Text(it, style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
    Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) { Text(stringResource(CoreUiR.string.models_model_details_screen_56)) }
    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text(stringResource(CoreUiR.string.models_model_details_screen_57)) }
}

@Composable
private fun ModelTransferNetworkPolicy.detailsNetworkLabel(): String = stringResource(
    if (this == ModelTransferNetworkPolicy.WIFI_ONLY) CoreUiR.string.models_network_wifi_only else CoreUiR.string.models_network_any,
)
