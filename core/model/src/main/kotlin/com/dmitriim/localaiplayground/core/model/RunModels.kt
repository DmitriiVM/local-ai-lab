package com.dmitriim.localaiplayground.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class RunStatus { SUCCEEDED, CANCELLED, FAILED }

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

@Serializable
enum class ConversationKind { CHAT, VOICE }

@Serializable
enum class ConversationMessageRole { USER, ASSISTANT }

@Serializable
data class ConversationRecord(
    val id: String,
    val kind: ConversationKind,
    val title: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

@Serializable
data class ConversationMessageRecord(
    val id: String,
    val conversationId: String,
    val role: ConversationMessageRole,
    val content: String,
    val createdAtEpochMs: Long,
    val incomplete: Boolean = false,
)

data class StorageUsage(
    val modelsBytes: Long = 0,
    val recordingsBytes: Long = 0,
    val generatedAudioBytes: Long = 0,
    val historyBytes: Long = 0,
)
