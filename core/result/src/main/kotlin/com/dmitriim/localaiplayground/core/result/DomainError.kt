package com.dmitriim.localaiplayground.core.result

import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.manifest.ModelId

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
