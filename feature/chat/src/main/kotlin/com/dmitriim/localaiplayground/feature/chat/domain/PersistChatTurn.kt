package com.dmitriim.localaiplayground.feature.chat.domain

import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.conversation.ConversationKind
import com.dmitriim.localaiplayground.core.model.conversation.ConversationMessageRecord
import com.dmitriim.localaiplayground.core.model.conversation.ConversationMessageRole
import com.dmitriim.localaiplayground.core.model.conversation.ConversationRecord
import com.dmitriim.localaiplayground.core.model.runs.RunRecord
import com.dmitriim.localaiplayground.core.model.service.RunRepository
import dev.zacsweers.metro.Inject
import kotlinx.serialization.json.Json
import java.util.UUID

@Inject
class PersistChatTurn(private val runRepository: RunRepository) {
    suspend operator fun invoke(snapshot: ChatPersistenceSnapshot) {
        runRepository.saveRun(
            RunRecord(
                id = UUID.randomUUID().toString(),
                capability = AiCapability.CHAT,
                status = snapshot.status,
                startedAtEpochMs = snapshot.startedAtEpochMs,
                completedAtEpochMs = System.currentTimeMillis(),
                model = snapshot.model,
                input = snapshot.input,
                output = snapshot.output,
                parametersJson = Json.encodeToString(snapshot.settings.toJson()),
                metricsJson = snapshot.metrics?.let { Json.encodeToString(it.toJson()) } ?: "{}",
                errorMessage = snapshot.errorMessage,
            ),
        )
        if (snapshot.messages.isEmpty()) return

        val now = System.currentTimeMillis()
        runRepository.saveConversation(
            ConversationRecord(
                id = snapshot.conversationId,
                kind = ConversationKind.CHAT,
                title = snapshot.messages.firstOrNull { it.role == ConversationMessageRole.USER }?.content?.take(48) ?: "Chat conversation",
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
            snapshot.messages.map { message ->
                ConversationMessageRecord(
                    id = message.id,
                    conversationId = snapshot.conversationId,
                    role = message.role,
                    content = message.content,
                    createdAtEpochMs = now,
                    incomplete = message.incomplete,
                )
            },
        )
    }
}
