package com.dmitriim.localaiplayground.source.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "runs",
    indices = [Index("completedAtEpochMs"), Index("capability"), Index("status"), Index("modelId")],
)
data class RunEntity(
    @PrimaryKey val id: String,
    val capability: String,
    val status: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val modelId: String?,
    val modelDisplayName: String?,
    val engineId: String?,
    val modelRevision: String?,
    val input: String?,
    val output: String?,
    val parametersJson: String,
    val metricsJson: String,
    val errorMessage: String?,
    val linkedRunIdsJson: String,
)

@Entity(tableName = "conversations", indices = [Index("updatedAtEpochMs")])
data class ConversationEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val title: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "conversation_messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("conversationId"), Index("createdAtEpochMs")],
)
data class ConversationMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val createdAtEpochMs: Long,
    val incomplete: Boolean,
)
