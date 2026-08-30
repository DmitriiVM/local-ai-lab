package com.dmitriim.localailab.feature.runs.api.data

import com.dmitriim.localailab.feature.runs.api.domain.conversation.ConversationMessageRecord
import com.dmitriim.localailab.feature.runs.api.domain.conversation.ConversationRecord
import com.dmitriim.localailab.feature.runs.api.domain.history.RunRecord
import com.dmitriim.localailab.feature.runs.api.domain.storage.StorageUsage
import kotlinx.coroutines.flow.Flow

interface RunRepository {
    val runs: Flow<List<RunRecord>>
    val conversations: Flow<List<ConversationRecord>>

    fun observeRun(id: String): Flow<RunRecord?>
    fun observeMessages(conversationId: String): Flow<List<ConversationMessageRecord>>
    suspend fun saveRun(record: RunRecord)
    suspend fun saveConversation(
        record: ConversationRecord,
        messages: List<ConversationMessageRecord>,
    )
    suspend fun deleteConversation(id: String)
    suspend fun clearRuns()
    suspend fun storageUsage(): StorageUsage
}
