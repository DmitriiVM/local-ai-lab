package com.dmitriim.localaiplayground.ai.api.llm

import com.dmitriim.localaiplayground.core.model.engine.ComputePreference
import com.dmitriim.localaiplayground.core.model.runtime.ChatModelReference

data class LlmLoadRequest(
    val model: ChatModelReference,
    val options: LlmLoadOptions = LlmLoadOptions(),
)

data class LlmLoadOptions(
    val contextSize: Int? = null,
    val threadCount: Int? = null,
    val computePreference: ComputePreference = ComputePreference.AUTO,
)

data class LlmLoadResult(
    val effectiveComputePreference: ComputePreference,
    val loadDurationMs: Long,
    val coldStart: Boolean,
    val diagnostics: LlmRuntimeDiagnostics = LlmRuntimeDiagnostics(),
)

data class LlmRuntimeDiagnostics(
    /** Runtime-reported compute implementation; this is diagnostic text, not a portable option. */
    val computeDetail: String? = null,
    val effectiveThreadCount: Int? = null,
    val systemInfo: String? = null,
    val fallbackReason: String? = null,
)
