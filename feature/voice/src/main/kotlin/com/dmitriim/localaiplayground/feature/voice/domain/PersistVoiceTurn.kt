package com.dmitriim.localaiplayground.feature.voice.domain

import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.conversation.ConversationKind
import com.dmitriim.localaiplayground.core.model.conversation.ConversationMessageRecord
import com.dmitriim.localaiplayground.core.model.conversation.ConversationMessageRole
import com.dmitriim.localaiplayground.core.model.conversation.ConversationRecord
import com.dmitriim.localaiplayground.core.model.runs.RunRecord
import com.dmitriim.localaiplayground.core.model.runs.RunStatus
import com.dmitriim.localaiplayground.core.model.service.RunRepository
import dev.zacsweers.metro.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

@Inject
class PersistVoiceTurn(private val runRepository: RunRepository) {
    suspend operator fun invoke(turn: CompletedVoiceTurn) {
        val settingsJson = Json.encodeToString(buildJsonObject {
            put("language", turn.request.languageCode)
            put("systemPrompt", turn.request.systemPrompt)
            put("temperature", turn.request.temperature)
            put("maxOutputTokens", turn.request.maxOutputTokens)
            put("contextSize", turn.request.contextSize)
            put("sttThreadCount", turn.request.sttThreadCount)
            put("llmThreadCount", turn.request.llmThreadCount)
            put("ttsThreadCount", turn.request.ttsThreadCount)
            put("speakerId", turn.request.speakerId)
            put("speechRate", turn.request.speechRate)
            put("volume", turn.request.volume)
        })
        val now = System.currentTimeMillis()
        val metrics = turn.metrics
        val componentIds = metrics.componentRunIds

        runRepository.saveRun(
            RunRecord(
                id = UUID.randomUUID().toString(),
                capability = AiCapability.VOICE_ASSISTANT,
                status = RunStatus.SUCCEEDED,
                startedAtEpochMs = turn.startedAtEpochMs,
                completedAtEpochMs = now,
                input = turn.transcript,
                output = turn.response,
                parametersJson = settingsJson,
                metricsJson = Json.encodeToString(buildJsonObject {
                    put("listeningDurationMs", metrics.listeningDurationMs)
                    put("speechFinalizationDurationMs", metrics.speechFinalizationDurationMs)
                    put("sttProcessingDurationMs", metrics.sttProcessingDurationMs)
                    put("llmCompletionDurationMs", metrics.llmCompletionDurationMs)
                    put("ttsCompletionDurationMs", metrics.ttsCompletionDurationMs)
                    put("endToEndTimeToFirstOutputMs", metrics.endToEndTimeToFirstOutputMs)
                }),
                linkedRunIds = listOf(componentIds.stt, componentIds.llm, componentIds.tts),
            ),
        )
        listOf(
            RunRecord(
                componentIds.stt, AiCapability.SPEECH_TO_TEXT, RunStatus.SUCCEEDED, turn.startedAtEpochMs, now,
                turn.speechModel, turn.transcript, turn.transcript, settingsJson,
                Json.encodeToString(buildJsonObject { put("processingDurationMs", metrics.sttProcessingDurationMs) }),
            ),
            RunRecord(
                componentIds.llm, AiCapability.CHAT, RunStatus.SUCCEEDED, turn.startedAtEpochMs, now,
                turn.chatModel, turn.transcript, turn.response, settingsJson,
                Json.encodeToString(buildJsonObject { put("completionDurationMs", metrics.llmCompletionDurationMs) }),
            ),
            RunRecord(
                componentIds.tts, AiCapability.TEXT_TO_SPEECH, RunStatus.SUCCEEDED, turn.startedAtEpochMs, now,
                turn.voiceModel, turn.response, "Generated speech", settingsJson,
                Json.encodeToString(buildJsonObject { put("completionDurationMs", metrics.ttsCompletionDurationMs) }),
            ),
        ).forEach { runRepository.saveRun(it) }

        if (turn.conversation.isNotEmpty()) {
            runRepository.saveConversation(
                ConversationRecord(
                    turn.conversationId,
                    ConversationKind.VOICE,
                    turn.conversation.first().userText.take(48),
                    now,
                    now,
                ),
                turn.conversation.flatMap { entry ->
                    listOf(
                        ConversationMessageRecord("${entry.id}-user", turn.conversationId, ConversationMessageRole.USER, entry.userText, now),
                        ConversationMessageRecord("${entry.id}-assistant", turn.conversationId, ConversationMessageRole.ASSISTANT, entry.assistantText, now),
                    )
                },
            )
        }
    }
}
