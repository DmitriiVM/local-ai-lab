package com.dmitriim.localaiplayground.feature.chat.domain

import com.dmitriim.localaiplayground.core.model.AiCapability
import com.dmitriim.localaiplayground.core.model.ConversationKind
import com.dmitriim.localaiplayground.core.model.ConversationMessageRecord
import com.dmitriim.localaiplayground.core.model.ConversationMessageRole
import com.dmitriim.localaiplayground.core.model.ConversationRecord
import com.dmitriim.localaiplayground.core.model.RunModelSnapshot
import com.dmitriim.localaiplayground.core.model.RunRecord
import com.dmitriim.localaiplayground.core.model.RunRepository
import com.dmitriim.localaiplayground.core.model.RunStatus
import dev.zacsweers.metro.Inject
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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

data class ChatPersistenceSnapshot(
    val conversationId: String,
    val status: RunStatus,
    val startedAtEpochMs: Long,
    val model: RunModelSnapshot?,
    val input: String,
    val output: String?,
    val settings: ChatRunSettings,
    val metrics: ChatRunMetrics?,
    val errorMessage: String?,
    val messages: List<ChatConversationSnapshot>,
)

data class ChatRunSettings(
    val systemPrompt: String,
    val temperature: Float,
    val topK: Int,
    val topP: Float,
    val maxOutputTokens: Int,
    val seed: Int,
    val contextSize: Int,
    val threadCount: Int,
) {
    fun toJson() = buildJsonObject {
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
    val promptTokens: Int,
    val timeToFirstTokenMs: Long?,
    val generatedTokens: Int,
    val totalDurationMs: Long,
    val finishReason: String,
    val effectiveThreadCount: Int,
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
    }
}

data class ChatConversationSnapshot(
    val id: String,
    val role: ConversationMessageRole,
    val content: String,
    val incomplete: Boolean,
)
