package com.dmitriim.localailab.ai.api.chat

import com.dmitriim.localailab.ai.api.engine.ComputePreference
import com.dmitriim.localailab.ai.api.model.runtime.ChatModelReference

/** Selects a local model and optional runtime configuration for [ChatExecution.load]. */
data class LlmLoadRequest(
    val model: ChatModelReference,
    val options: LlmLoadOptions = LlmLoadOptions(),
)

/** Optional model-load controls. Null values allow the selected runtime to choose defaults. */
data class LlmLoadOptions(
    val contextSize: Int? = null,
    val threadCount: Int? = null,
    val computePreference: ComputePreference = ComputePreference.AUTO,
)

/** Effective configuration and diagnostics returned by a completed model load. */
data class LlmLoadResult(
    val effectiveComputePreference: ComputePreference,
    val loadDurationMs: Long,
    val coldStart: Boolean,
    val diagnostics: LlmRuntimeDiagnostics = LlmRuntimeDiagnostics(),
)

/** Non-portable diagnostic information reported by a concrete runtime. */
data class LlmRuntimeDiagnostics(
    /** Runtime-reported compute implementation; this is diagnostic text, not a portable option. */
    val computeDetail: String? = null,
    val effectiveThreadCount: Int? = null,
    val systemInfo: String? = null,
    val fallbackReason: String? = null,
)
