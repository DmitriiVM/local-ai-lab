package com.dmitriim.localailab.feature.models.impl.domain.transfer

import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Derives a smoothed transfer rate and remaining-time estimate from streamed progress samples. */
internal class ModelTransferThroughputEstimator {
    private val samples = mutableMapOf<ModelId, TransferSample>()
    private val mutableEstimates = MutableStateFlow<Map<ModelId, ModelTransferThroughput>>(emptyMap())

    val estimates = mutableEstimates.asStateFlow()

    fun record(progress: ModelTransferProgress) {
        val modelId = progress.modelId
        val previous = samples[modelId]
        samples[modelId] = TransferSample(
            executionGeneration = progress.executionGeneration,
            completedBytes = progress.completedBytes,
            recordedAtElapsedMs = progress.recordedAtElapsedMs,
        )
        if (previous == null || previous.executionGeneration != progress.executionGeneration) return
        val elapsedMillis = progress.recordedAtElapsedMs - previous.recordedAtElapsedMs
        val transferredBytes = progress.completedBytes - previous.completedBytes
        if (elapsedMillis <= 0L || transferredBytes <= 0L) return
        val instantaneousRate = (
            transferredBytes.toDouble() * MILLIS_PER_SECOND / elapsedMillis
            ).toLong().coerceAtLeast(1L)
        val previousRate = mutableEstimates.value[modelId]?.bytesPerSecond
        val smoothedRate = previousRate?.let { previous ->
            (previous * PREVIOUS_RATE_WEIGHT + instantaneousRate) / RATE_WEIGHT_SUM
        } ?: instantaneousRate
        val remainingBytes = (progress.totalBytes - progress.completedBytes).coerceAtLeast(0L)
        val remainingMillis = if (remainingBytes == 0L) {
            0L
        } else {
            (remainingBytes.toDouble() * MILLIS_PER_SECOND / smoothedRate).toLong().coerceAtLeast(0L)
        }
        mutableEstimates.update {
            it + (modelId to ModelTransferThroughput(smoothedRate, remainingMillis))
        }
    }

    fun clear(modelId: ModelId) {
        samples.remove(modelId)
        mutableEstimates.update { it - modelId }
    }

    private data class TransferSample(
        val executionGeneration: Long,
        val completedBytes: Long,
        val recordedAtElapsedMs: Long,
    )

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
        const val PREVIOUS_RATE_WEIGHT = 3L
        const val RATE_WEIGHT_SUM = PREVIOUS_RATE_WEIGHT + 1L
    }
}
