package com.dmitriim.localailab.feature.models.impl.details.presentation

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.feature.models.api.domain.library.InstalledModel
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferState
import com.dmitriim.localailab.feature.models.impl.models.presentation.ModelDownloadButton

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
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (installedModel != null) {
                InstalledModelActions(validating, onValidate, onDelete)
            } else {
                when (transfer) {
                    is ModelTransferState.Queued -> TransferQueuedActions(
                        transfer = transfer,
                        onPause = onPauseTransfer,
                        onCancel = { confirmCancel = true },
                        onUseAnyNetwork = { confirmAnyNetwork = true },
                    )
                    is ModelTransferState.Running -> TransferRunningActions(
                        transfer = transfer,
                        onPause = onPauseTransfer,
                        onCancel = { confirmCancel = true },
                    )
                    is ModelTransferState.Paused -> TransferPausedActions(
                        transfer = transfer,
                        onResume = onResumeOnWifi,
                        onCancel = { confirmCancel = true },
                    )
                    ModelTransferState.Installing,
                    ModelTransferState.Completed,
                    -> Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(CoreUiR.string.models_model_details_screen_58))
                    }
                    is ModelTransferState.Failed -> {
                        Text(transfer.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(CoreUiR.string.models_model_details_screen_59))
                        }
                    }
                    ModelTransferState.Idle,
                    null,
                    -> ModelDownloadButton(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
    TransferConfirmationDialogs(
        confirmAnyNetwork = confirmAnyNetwork,
        confirmCancel = confirmCancel,
        onDismissAnyNetwork = { confirmAnyNetwork = false },
        onConfirmAnyNetwork = {
            confirmAnyNetwork = false
            onResumeOnAnyNetwork()
        },
        onDismissCancel = { confirmCancel = false },
        onConfirmCancel = {
            confirmCancel = false
            onCancelTransfer()
        },
    )
}

@Composable
private fun InstalledModelActions(
    validating: Boolean,
    onValidate: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onValidate,
            enabled = !validating,
            modifier = Modifier.weight(1f),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.58f),
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.tertiary,
            ),
        ) {
            Text(
                stringResource(
                    if (validating) CoreUiR.string.models_validating else CoreUiR.string.models_validate,
                ),
            )
        }
        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text(stringResource(CoreUiR.string.models_model_details_screen_50))
        }
    }
}

@Composable
private fun TransferConfirmationDialogs(
    confirmAnyNetwork: Boolean,
    confirmCancel: Boolean,
    onDismissAnyNetwork: () -> Unit,
    onConfirmAnyNetwork: () -> Unit,
    onDismissCancel: () -> Unit,
    onConfirmCancel: () -> Unit,
) {
    if (confirmAnyNetwork) {
        AlertDialog(
            onDismissRequest = onDismissAnyNetwork,
            title = { Text(stringResource(CoreUiR.string.models_model_details_screen_60)) },
            text = { Text(stringResource(CoreUiR.string.models_model_details_screen_61)) },
            confirmButton = {
                Button(onClick = onConfirmAnyNetwork) {
                    Text(stringResource(CoreUiR.string.models_model_details_screen_62))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissAnyNetwork) {
                    Text(stringResource(CoreUiR.string.models_model_details_screen_63))
                }
            },
        )
    }
    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = onDismissCancel,
            title = { Text(stringResource(CoreUiR.string.models_model_details_screen_64)) },
            text = { Text(stringResource(CoreUiR.string.models_model_details_screen_65)) },
            confirmButton = {
                Button(onClick = onConfirmCancel) {
                    Text(stringResource(CoreUiR.string.models_model_details_screen_66))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissCancel) {
                    Text(stringResource(CoreUiR.string.models_model_details_screen_67))
                }
            },
        )
    }
}
