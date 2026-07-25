package com.dmitriim.localaiplayground.core.model

data class ModelImportRequest(
    val displayName: String,
    val engineId: EngineId,
    val profileType: RuntimeProfileType,
    val documentUris: List<String>,
)

sealed interface ModelTransferState {
    data object Idle : ModelTransferState
    data object Queued : ModelTransferState
    data class Running(val completedBytes: Long, val totalBytes: Long?) : ModelTransferState
    data object Installing : ModelTransferState
    data class Failed(val message: String) : ModelTransferState
    data object Cancelled : ModelTransferState
    data object Completed : ModelTransferState
}
