package com.dmitriim.localailab.feature.assistant.impl.domain

import com.dmitriim.localailab.ai.api.engine.ComputePreference
import com.dmitriim.localailab.ai.api.profiling.InferenceTelemetry
import com.dmitriim.localailab.ai.api.profiling.serialization.putInferenceTelemetry
import com.dmitriim.localailab.feature.runs.api.domain.conversation.ConversationMessageRole
import com.dmitriim.localailab.feature.runs.api.domain.history.RunModelSnapshot
import com.dmitriim.localailab.feature.runs.api.domain.history.RunStatus
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class ChatPersistenceSnapshot(
    val runId: String,
    val conversationId: String,
    val status: RunStatus,
    val startedAtEpochMs: Long,
    val model: RunModelSnapshot?,
    val input: String,
    val output: String?,
    val settings: ChatRunSettings,
    val metrics: ChatRunMetrics?,
    val errorMessage: String?,
    val messages: List<AssistantConversationSnapshot>,
)

data class ChatRunSettings(
    val computePreference: ComputePreference,
    val systemPrompt: String,
    val temperature: Float,
    val topK: Int,
    val topP: Float,
    val maxOutputTokens: Int,
    val seed: Int?,
    val contextSize: Int,
    val threadCount: Int,
) {
    fun toJson() = buildJsonObject {
        put("computePreference", computePreference.name)
        put("systemPrompt", systemPrompt)
        put("temperature", temperature)
        put("topK", topK)
        put("topP", topP)
        put("maxOutputTokens", maxOutputTokens)
        put("seed", seed)
        put("contextSize", contextSize)
        put("threadCount", threadCount)
    }
}

data class ChatRunMetrics(
    val coldStart: Boolean,
    val loadDurationMs: Long,
    val promptTokens: Int?,
    val timeToFirstTokenMs: Long?,
    val generatedTokens: Int?,
    val totalDurationMs: Long,
    val finishReason: String,
    val effectiveThreadCount: Int?,
    val telemetry: InferenceTelemetry? = null,
) {
    fun toJson() = buildJsonObject {
        put("coldStart", coldStart)
        put("loadDurationMs", loadDurationMs)
        put("promptTokens", promptTokens)
        put("timeToFirstTokenMs", timeToFirstTokenMs)
        put("generatedTokens", generatedTokens)
        put("totalDurationMs", totalDurationMs)
        put("finishReason", finishReason)
        put("effectiveThreadCount", effectiveThreadCount)
        putInferenceTelemetry(telemetry)
    }
}

data class AssistantConversationSnapshot(
    val id: String,
    val role: ConversationMessageRole,
    val content: String,
    val incomplete: Boolean,
)
