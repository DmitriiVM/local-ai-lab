package com.dmitriim.localailab.feature.assistant.impl.domain

import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.feature.runs.api.data.RunRepository
import com.dmitriim.localailab.feature.runs.api.domain.conversation.ConversationKind
import com.dmitriim.localailab.feature.runs.api.domain.conversation.ConversationMessageRecord
import com.dmitriim.localailab.feature.runs.api.domain.conversation.ConversationMessageRole
import com.dmitriim.localailab.feature.runs.api.domain.conversation.ConversationRecord
import com.dmitriim.localailab.feature.runs.api.domain.history.RunRecord
import dev.zacsweers.metro.Inject
import kotlinx.serialization.json.Json

@Inject
class PersistAssistantTurn(private val runRepository: RunRepository) {
    suspend operator fun invoke(snapshot: ChatPersistenceSnapshot): String {
        val runId = snapshot.runId
        runRepository.saveRun(
            RunRecord(
                id = runId,
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
        if (snapshot.messages.isEmpty()) return runId

        val now = System.currentTimeMillis()
        runRepository.saveConversation(
            ConversationRecord(
                id = snapshot.conversationId,
                kind = ConversationKind.ASSISTANT,
                title = snapshot.messages.firstOrNull { it.role == ConversationMessageRole.USER }?.content?.take(48) ?: "Assistant conversation",
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
        return runId
    }
}
