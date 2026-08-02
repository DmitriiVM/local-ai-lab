package com.dmitriim.localaiplayground.core.model.conversation

import kotlinx.serialization.Serializable

@Serializable
enum class ConversationKind { ASSISTANT, }

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
