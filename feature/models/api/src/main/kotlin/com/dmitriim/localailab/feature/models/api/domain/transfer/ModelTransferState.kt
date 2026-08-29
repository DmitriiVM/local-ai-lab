package com.dmitriim.localailab.feature.models.api.domain.transfer

sealed interface ModelTransferState {
    data object Idle : ModelTransferState
    data class Queued(
        val completedBytes: Long,
        val totalBytes: Long,
        val networkPolicy: ModelTransferNetworkPolicy,
    ) : ModelTransferState
    data class Running(
        val completedBytes: Long,
        val totalBytes: Long,
        val networkPolicy: ModelTransferNetworkPolicy,
        val bytesPerSecond: Long? = null,
        val estimatedRemainingMillis: Long? = null,
    ) : ModelTransferState
    data class Paused(
        val completedBytes: Long,
        val totalBytes: Long,
        val networkPolicy: ModelTransferNetworkPolicy,
        val reason: String?,
    ) : ModelTransferState
    data object Installing : ModelTransferState
    data class Failed(val message: String) : ModelTransferState
    data object Completed : ModelTransferState
}
