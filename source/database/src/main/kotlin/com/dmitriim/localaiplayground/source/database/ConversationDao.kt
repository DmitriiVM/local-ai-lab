package com.dmitriim.localaiplayground.source.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversation_messages WHERE conversationId = :conversationId ORDER BY createdAtEpochMs")
    fun observeMessages(conversationId: String): Flow<List<ConversationMessageEntity>>

    @Upsert
    suspend fun upsertConversation(entity: ConversationEntity)

    @Upsert
    suspend fun upsertMessages(messages: List<ConversationMessageEntity>)

    @Query("DELETE FROM conversation_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessages(conversationId: String)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("DELETE FROM conversation_messages")
    suspend fun clearMessages()

    @Query("DELETE FROM conversations")
    suspend fun clearConversations()

    @Transaction
    suspend fun replaceConversation(entity: ConversationEntity, messages: List<ConversationMessageEntity>) {
        upsertConversation(entity)
        deleteMessages(entity.id)
        upsertMessages(messages)
    }
}
