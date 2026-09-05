package com.dmitriim.localailab.feature.models.impl.models.presentation

import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferState

internal data class ModelDownloadActivity(
    val runningDownloadCount: Int,
    val queuedDownloadCount: Int,
    val completedBytes: Long,
    val totalBytes: Long,
    val estimatedRemainingMillis: Long?,
) {
    val downloadCount: Int = runningDownloadCount + queuedDownloadCount
    val progress: Float = if (totalBytes > 0L) {
        (completedBytes.toDouble() / totalBytes.toDouble())
            .toFloat()
            .coerceIn(0f, 1f)
    } else {
        0f
    }
}

internal fun Map<ModelId, ModelTransferState>.toModelDownloadActivityOrNull(): ModelDownloadActivity? {
    var runningDownloadCount = 0
    var queuedDownloadCount = 0
    var completedBytes = 0L
    var totalBytes = 0L
    var bytesPerSecond = 0L

    values.forEach { transfer ->
        when (transfer) {
            is ModelTransferState.Queued -> {
                queuedDownloadCount += 1
                completedBytes += transfer.completedBytes
                totalBytes += transfer.totalBytes
            }
            is ModelTransferState.Running -> {
                runningDownloadCount += 1
                completedBytes += transfer.completedBytes
                totalBytes += transfer.totalBytes
                bytesPerSecond += transfer.bytesPerSecond?.coerceAtLeast(0L) ?: 0L
            }
            ModelTransferState.Completed,
            is ModelTransferState.Failed,
            ModelTransferState.Idle,
            ModelTransferState.Installing,
            is ModelTransferState.Paused,
            -> Unit
        }
    }

    val downloadCount = runningDownloadCount + queuedDownloadCount
    if (downloadCount == 0) {
        return null
    }

    val remainingBytes = (totalBytes - completedBytes).coerceAtLeast(0L)
    val estimatedRemainingMillis = if (bytesPerSecond > 0L) {
        (remainingBytes.toDouble() * MILLIS_PER_SECOND / bytesPerSecond.toDouble())
            .toLong()
            .coerceAtLeast(0L)
    } else {
        null
    }
    return ModelDownloadActivity(
        runningDownloadCount = runningDownloadCount,
        queuedDownloadCount = queuedDownloadCount,
        completedBytes = completedBytes,
        totalBytes = totalBytes,
        estimatedRemainingMillis = estimatedRemainingMillis,
    )
}

private const val MILLIS_PER_SECOND = 1_000L
