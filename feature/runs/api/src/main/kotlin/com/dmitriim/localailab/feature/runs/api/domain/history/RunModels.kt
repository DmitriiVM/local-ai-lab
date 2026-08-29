package com.dmitriim.localailab.feature.runs.api.domain.history

import com.dmitriim.localailab.core.model.capability.AiCapability
import kotlinx.serialization.Serializable

@Serializable
enum class RunStatus { SUCCEEDED, CANCELLED, FAILED }

@Serializable
enum class RunKind { INFERENCE, BENCHMARK_SESSION }

@Serializable
data class RunModelSnapshot(
    val modelId: String,
    val displayName: String,
    val engineId: String,
    val revision: String? = null,
)

/** An immutable, self-contained record of one local inference attempt. */
@Serializable
data class RunRecord(
    val id: String,
    val kind: RunKind = RunKind.INFERENCE,
    val benchmarkSessionId: String? = null,
    val capability: AiCapability,
    val status: RunStatus,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val model: RunModelSnapshot? = null,
    val input: String? = null,
    val output: String? = null,
    val parametersJson: String = "{}",
    val metricsJson: String = "{}",
    val errorMessage: String? = null,
    val linkedRunIds: List<String> = emptyList(),
)
