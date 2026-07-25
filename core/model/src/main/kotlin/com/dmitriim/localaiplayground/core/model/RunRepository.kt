package com.dmitriim.localaiplayground.core.model

import kotlinx.coroutines.flow.Flow

interface RunRepository {
    val runs: Flow<List<RunRecord>>
    val conversations: Flow<List<ConversationRecord>>

    fun observeRun(id: String): Flow<RunRecord?>
    fun observeMessages(conversationId: String): Flow<List<ConversationMessageRecord>>
    suspend fun saveRun(record: RunRecord)
    suspend fun saveConversation(record: ConversationRecord, messages: List<ConversationMessageRecord>)
    suspend fun deleteConversation(id: String)
    suspend fun clearHistory()
    suspend fun storageUsage(): StorageUsage
}
