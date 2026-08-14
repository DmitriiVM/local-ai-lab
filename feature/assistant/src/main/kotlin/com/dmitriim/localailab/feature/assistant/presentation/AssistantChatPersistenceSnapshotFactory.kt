package com.dmitriim.localailab.feature.assistant.presentation

import com.dmitriim.localailab.core.model.conversation.ConversationMessageRole
import com.dmitriim.localailab.core.model.runs.RunModelSnapshot
import com.dmitriim.localailab.core.model.runs.RunStatus
import com.dmitriim.localailab.feature.assistant.domain.AssistantConversationSnapshot
import com.dmitriim.localailab.feature.assistant.domain.ChatPersistenceSnapshot
import com.dmitriim.localailab.feature.assistant.domain.ChatRunMetrics
import com.dmitriim.localailab.feature.assistant.domain.ChatRunSettings

internal object AssistantChatPersistenceSnapshotFactory {
    fun create(
        runId: String,
        conversationId: String,
        status: RunStatus,
        startedAt: Long,
        model: ChatModelOption?,
        input: String,
        output: String?,
        settings: EffectiveChatSettings,
        metrics: ChatMetrics?,
        error: String?,
        messages: List<ChatMessage>,
        incompleteAssistant: Boolean = false,
    ) = ChatPersistenceSnapshot(
        runId = runId,
        conversationId = conversationId,
        status = status,
        startedAtEpochMs = startedAt,
        model = model?.let { RunModelSnapshot(it.id.value, it.displayName, it.engineId.value) },
        input = input,
        output = output,
        settings = ChatRunSettings(
            computePreference = settings.computePreference,
            systemPrompt = settings.systemPrompt,
            temperature = settings.temperature,
            topK = settings.topK,
            topP = settings.topP,
            maxOutputTokens = settings.maxOutputTokens,
            seed = settings.seed,
            contextSize = settings.contextSize,
            threadCount = settings.threadCount,
        ),
        metrics = metrics?.let {
            ChatRunMetrics(
                it.coldStart,
                it.loadDurationMs,
                it.promptTokens,
                it.timeToFirstTokenMs,
                it.generatedTokens,
                it.totalDurationMs,
                it.finishReason.name,
                it.effectiveThreadCount,
                it.telemetry,
            )
        },
        errorMessage = error,
        messages = messages.map { message ->
            AssistantConversationSnapshot(
                id = message.id,
                role = if (message.role == ChatMessageRole.USER) {
                    ConversationMessageRole.USER
                } else {
                    ConversationMessageRole.ASSISTANT
                },
                content = message.content,
                incomplete = message.streaming ||
                    (incompleteAssistant && message.role == ChatMessageRole.ASSISTANT && message.failed),
            )
        },
    )
}
