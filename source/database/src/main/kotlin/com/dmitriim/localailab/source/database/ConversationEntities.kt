package com.dmitriim.localailab.source.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
