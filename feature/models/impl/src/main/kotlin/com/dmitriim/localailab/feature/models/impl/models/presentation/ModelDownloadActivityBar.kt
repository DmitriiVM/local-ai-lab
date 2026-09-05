package com.dmitriim.localailab.feature.models.impl.models.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.core.ui.R as CoreUiR

internal val ModelDownloadActivityBarHeight = 64.dp
internal val ModelDownloadActivityBarBottomOffset = 24.dp
internal val ModelDownloadActivityBarClearance =
    ModelDownloadActivityBarHeight + ModelDownloadActivityBarBottomOffset

@Composable
internal fun ModelDownloadActivityBar(
    activity: ModelDownloadActivity,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(ModelDownloadActivityBarHeight),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = activity.title(),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = "${(activity.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                text = stringResource(
                    CoreUiR.string.models_download_activity_progress,
                    activity.completedBytes.toReadableBytes(),
                    activity.totalBytes.toReadableBytes(),
                    activity.remainingTimeLabel(),
                ),
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
            )
            LinearProgressIndicator(
                progress = { activity.progress },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ModelDownloadActivity.title(): String = when {
    runningDownloadCount == 0 -> {
        pluralStringResource(
            CoreUiR.plurals.models_download_activity_waiting,
            downloadCount,
            downloadCount,
        )
    }
    else -> {
        pluralStringResource(
            CoreUiR.plurals.models_download_activity_in_progress,
            downloadCount,
            downloadCount,
        )
    }
}

@Composable
private fun ModelDownloadActivity.remainingTimeLabel(): String = estimatedRemainingMillis?.let {
    stringResource(
        CoreUiR.string.models_download_activity_remaining,
        it.toModelTransferRemainingDuration(),
    )
} ?: if (runningDownloadCount > 0) {
    stringResource(CoreUiR.string.models_download_activity_calculating)
} else {
    stringResource(CoreUiR.string.models_download_activity_waiting_time)
}
