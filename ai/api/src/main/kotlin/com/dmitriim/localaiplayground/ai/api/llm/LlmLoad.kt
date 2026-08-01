package com.dmitriim.localaiplayground.ai.api.llm

import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileId

data class LlmLoadRequest(
    val profileType: ModelProfileId,
    val modelPath: String,
    val contextSize: Int = 512,
    val threadCount: Int = 0,
    val requestedBackend: LlmBackend = LlmBackend.CPU,
)

data class LlmLoadResult(
    val effectiveBackend: LlmBackend,
    val effectiveThreadCount: Int,
    val loadDurationMs: Long,
    val systemInfo: String,
    val coldStart: Boolean,
)

enum class LlmBackend {
    CPU,
    VULKAN,
    NNAPI,
}
