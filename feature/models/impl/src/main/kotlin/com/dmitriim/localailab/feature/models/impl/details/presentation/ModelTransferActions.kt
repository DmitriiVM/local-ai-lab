package com.dmitriim.localailab.feature.models.impl.details.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferNetworkPolicy
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferState

@Composable
internal fun TransferQueuedActions(
    transfer: ModelTransferState.Queued,
    onPause: () -> Unit,
    onCancel: () -> Unit,
    onUseAnyNetwork: () -> Unit,
) {
    Text(
        stringResource(
            CoreUiR.string.models_model_details_screen_format_7,
            transfer.networkPolicy.detailsNetworkLabel(),
        ),
    )
    Text(
        stringResource(CoreUiR.string.models_model_download_battery_requirement),
        style = MaterialTheme.typography.bodySmall,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f)) {
            Text(stringResource(CoreUiR.string.models_model_details_screen_51))
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
            Text(stringResource(CoreUiR.string.models_model_details_screen_52))
        }
    }
    if (transfer.networkPolicy == ModelTransferNetworkPolicy.WIFI_ONLY) {
        OutlinedButton(onClick = onUseAnyNetwork, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(CoreUiR.string.models_model_details_screen_53))
        }
    }
}

@Composable
internal fun TransferRunningActions(
    transfer: ModelTransferState.Running,
    onPause: () -> Unit,
    onCancel: () -> Unit,
) {
    Text(
        stringResource(
            CoreUiR.string.models_model_details_screen_format_8,
            transfer.completedBytes.toDetailsReadableBytes(),
            transfer.totalBytes.toDetailsReadableBytes(),
        ),
    )
    transfer.bytesPerSecond?.takeIf { it > 0L }?.let { bytesPerSecond ->
        val remaining = transfer.estimatedRemainingMillis?.toDetailsRemainingDuration() ?: return@let
        Text(
            stringResource(
                CoreUiR.string.models_model_transfer_speed_eta,
                bytesPerSecond.toDetailsReadableBytes(),
                remaining,
            ),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f)) {
            Text(stringResource(CoreUiR.string.models_model_details_screen_54))
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
            Text(stringResource(CoreUiR.string.models_model_details_screen_55))
        }
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
internal fun TransferPausedActions(
    transfer: ModelTransferState.Paused,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    Text(
        stringResource(
            CoreUiR.string.models_model_details_screen_format_9,
            transfer.completedBytes.toDetailsReadableBytes(),
            transfer.totalBytes.toDetailsReadableBytes(),
        ),
    )
    transfer.reason?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(CoreUiR.string.models_model_details_screen_56))
    }
    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(CoreUiR.string.models_model_details_screen_57))
    }
}

@Composable
private fun ModelTransferNetworkPolicy.detailsNetworkLabel(): String = stringResource(
    if (this == ModelTransferNetworkPolicy.WIFI_ONLY) {
        CoreUiR.string.models_network_wifi_only
    } else {
        CoreUiR.string.models_network_any
    },
)
