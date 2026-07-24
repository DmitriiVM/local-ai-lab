package com.dmitriim.localaiplayground.core.result

import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.core.model.ModelId

sealed interface OperationState<out T> {
    data object Idle : OperationState<Nothing>
    data class Preparing(val message: String) : OperationState<Nothing>
    data class Running(
        val message: String,
        val completed: Long? = null,
        val total: Long? = null,
    ) : OperationState<Nothing>

    data class Cancelling(val message: String) : OperationState<Nothing>
    data class Completed<T>(val value: T) : OperationState<T>
    data class Error(val error: DomainError) : OperationState<Nothing>
}

data class DomainError(
    val category: DomainErrorCategory,
    val title: String,
    val explanation: String,
    val suggestedAction: String? = null,
    val technicalDetails: String? = null,
    val engineId: EngineId? = null,
    val modelId: ModelId? = null,
    val retryable: Boolean = false,
)

enum class DomainErrorCategory {
    PERMISSION_DENIED,
    UNSUPPORTED_DEVICE,
    INCOMPATIBLE_MODEL,
    MISSING_MODEL,
    INSUFFICIENT_STORAGE,
    MEMORY_RISK,
    DOWNLOAD_FAILURE,
    INTEGRITY_FAILURE,
    ENGINE_UNAVAILABLE,
    ENGINE_INITIALIZATION,
    INFERENCE_FAILURE,
    AUDIO_FAILURE,
    CANCELLED,
    UNEXPECTED,
}

sealed interface LoadableState<out T> {
    data object Loading : LoadableState<Nothing>
    data class Content<T>(val value: T) : LoadableState<T>
    data class Empty(val title: String, val explanation: String) : LoadableState<Nothing>
    data class Unsupported(val title: String, val explanation: String) : LoadableState<Nothing>
    data class Error(val error: DomainError) : LoadableState<Nothing>
}
