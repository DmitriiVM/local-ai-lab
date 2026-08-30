package com.dmitriim.localailab.feature.assistant.impl.presentation

import com.dmitriim.localailab.ai.api.chat.ChatEngine
import com.dmitriim.localailab.feature.assistant.impl.domain.chat.ChatGenerationEvent
import com.dmitriim.localailab.feature.assistant.impl.domain.chat.ChatGenerationRequest
import com.dmitriim.localailab.feature.assistant.impl.domain.chat.GenerateAssistantResponse
import com.dmitriim.localailab.feature.assistant.impl.domain.chat.PersistAssistantTurn
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.AssistantInputMode
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.AssistantOperation
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.AssistantUiState
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.ChatMessage
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.ChatMessageRole
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.ChatMetrics
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.GenerationOutcome
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.SpeechOutcome
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.replaceAssistantText
import com.dmitriim.localailab.feature.runs.api.domain.history.RunStatus
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class AssistantChatController(
    private val host: AssistantOperationHost,
    private val chatEngine: ChatEngine,
    private val generateResponse: GenerateAssistantResponse,
    private val persistAssistantTurn: PersistAssistantTurn,
    private val ttsController: AssistantTtsController,
    private val conversationId: () -> String,
) {
    fun unloadRuntime() {
        if (!host.state.value.isIdle) return
        host.scope.launch(Dispatchers.Default) {
            runCatching { chatEngine.unload() }
                .onSuccess { host.state.update { it.copy(statusMessage = "Chat model unloaded.") } }
                .onFailure { error ->
                    host.state.update {
                        it.copy(errorMessage = error.message ?: "Could not unload the chat model.")
                    }
                }
        }
    }

    fun send() {
        val snapshot = host.state.value
        val input = snapshot.input.trim()
        if (input.isEmpty() || !snapshot.isIdle) return
        host.launchForeground {
            runCatching {
                generateInternal(
                    base = snapshot.messages,
                    userText = input,
                    appendUser = true,
                    speakAfter = snapshot.inputMode == AssistantInputMode.VOICE,
                )
            }.onFailure(host::handleOperationFailure)
        }
    }

    fun regenerate() {
        val snapshot = host.state.value
        val assistantIndex = snapshot.messages.indexOfLast { it.role == ChatMessageRole.ASSISTANT }
        if (assistantIndex < 0 || !snapshot.isIdle) return
        val user = snapshot.messages.subList(0, assistantIndex).lastOrNull { it.role == ChatMessageRole.USER } ?: return
        host.launchForeground {
            runCatching {
                generateInternal(
                    base = snapshot.messages.take(assistantIndex),
                    userText = user.content,
                    appendUser = false,
                    speakAfter = snapshot.inputMode == AssistantInputMode.VOICE,
                )
            }.onFailure(host::handleOperationFailure)
        }
    }

    suspend fun generateFromVoice(initial: AssistantUiState, userText: String): GenerationOutcome = generateInternal(
        base = initial.messages,
        userText = userText,
        appendUser = true,
        speakAfter = true,
    )

    fun cancel() {
        chatEngine.cancel()
    }

    @Suppress("LongMethod") // This serializes the state transitions of a single assistant operation.
    private suspend fun generateInternal(
        base: List<ChatMessage>,
        userText: String,
        appendUser: Boolean,
        speakAfter: Boolean,
    ): GenerationOutcome {
        val snapshot = host.state.value
        val selectedId = snapshot.selectedChatModelId ?: error("Select an installed chat model first.")
        val settings = snapshot.chatSettings.toEffective()
        val visibleMessages = if (appendUser) base + ChatMessage.user(userText) else base
        val startedAt = System.currentTimeMillis()
        val runId = UUID.randomUUID().toString()
        val model = snapshot.chatModels.firstOrNull { it.id == selectedId }
        var assistantId: String? = null
        try {
            host.state.update {
                it.copy(
                    input = if (appendUser) "" else it.input,
                    messages = visibleMessages,
                    operation = AssistantOperation.Loading,
                    errorMessage = null,
                    statusMessage = "Loading the local chat model…",
                    metrics = null,
                )
            }
            var completedText = ""
            generateResponse.execute(
                ChatGenerationRequest(
                    modelId = selectedId,
                    turns = visibleMessages.map(ChatMessage::toDomain),
                    config = settings.toDomain(),
                    runId = runId,
                ),
            ).collect { event ->
                when (event) {
                    is ChatGenerationEvent.Prepared -> {
                        assistantId = UUID.randomUUID().toString()
                        host.state.update {
                            it.copy(
                                operation = AssistantOperation.Generating,
                                statusMessage = "Generating locally…",
                                contextUsage = event.contextUsage.toUi(),
                                messages = visibleMessages + ChatMessage.assistant(
                                    id = requireNotNull(assistantId),
                                    content = "",
                                    streaming = true,
                                ),
                            )
                        }
                    }

                    is ChatGenerationEvent.Token -> assistantId?.let { id ->
                        host.state.update {
                            it.replaceAssistantText(id, event.text, append = true)
                        }
                    }

                    is ChatGenerationEvent.Completed -> {
                        val id = requireNotNull(assistantId) {
                            "The chat engine completed without preparing a response."
                        }
                        val result = event.generation
                        completedText = result.text
                        val metrics = ChatMetrics(
                            event.modelName,
                            event.load.coldStart,
                            event.load.loadDurationMs,
                            result.promptTokenCount,
                            rate(result.promptTokenCount, result.promptDurationMs),
                            result.firstTokenLatencyMs,
                            result.generatedTokenCount,
                            rate(result.generatedTokenCount, result.generationDurationMs),
                            result.totalDurationMs,
                            result.finishReason,
                            settings,
                            event.load.diagnostics.effectiveThreadCount,
                            event.telemetry,
                        )
                        host.state.update { current ->
                            current.replaceAssistantText(id, result.text, append = false).copy(
                                operation = AssistantOperation.Idle,
                                metrics = metrics,
                                statusMessage = null,
                            )
                        }
                        host.activeLinkedRunIds += persistAssistantTurn(
                            AssistantChatPersistenceSnapshotFactory.create(
                                runId = runId,
                                conversationId = conversationId(),
                                status = RunStatus.SUCCEEDED,
                                startedAt = startedAt,
                                model = model,
                                input = userText,
                                output = result.text,
                                settings = settings,
                                metrics = metrics,
                                error = null,
                                messages = host.state.value.messages,
                            ),
                        )
                    }
                }
            }
            val speech = if (speakAfter) {
                ttsController.speakInternal(completedText, requireNotNull(assistantId))
            } else {
                SpeechOutcome()
            }
            return GenerationOutcome(completedText, speech.succeeded, speech.error)
        } catch (cancelled: CancellationException) {
            host.state.update { current ->
                current.copy(
                    operation = AssistantOperation.Idle,
                    speakingMessageId = null,
                    statusMessage = "Generation stopped.",
                    messages = current.messages.map { message ->
                        if (message.streaming) {
                            message.copy(streaming = false, failed = true)
                        } else {
                            message
                        }
                    },
                )
            }
            val partial = host.state.value.messages.lastOrNull { it.role == ChatMessageRole.ASSISTANT }?.content
            withContext(NonCancellable) {
                host.activeLinkedRunIds += persistAssistantTurn(
                    AssistantChatPersistenceSnapshotFactory.create(
                        runId = runId,
                        conversationId = conversationId(),
                        status = RunStatus.CANCELLED,
                        startedAt = startedAt,
                        model = model,
                        input = userText,
                        output = partial,
                        settings = settings,
                        metrics = null,
                        error = "Generation cancelled.",
                        messages = host.state.value.messages,
                        incompleteAssistant = true,
                    ),
                )
            }
            throw cancelled
        } catch (error: Throwable) {
            val message = error.message ?: "Local generation failed."
            host.state.update { current ->
                current.copy(
                    operation = AssistantOperation.Idle,
                    speakingMessageId = null,
                    errorMessage = message,
                    messages = current.messages.map { currentMessage ->
                        if (currentMessage.streaming) {
                            currentMessage.copy(streaming = false, failed = true)
                        } else {
                            currentMessage
                        }
                    },
                )
            }
            withContext(NonCancellable) {
                host.activeLinkedRunIds += persistAssistantTurn(
                    AssistantChatPersistenceSnapshotFactory.create(
                        runId = runId,
                        conversationId = conversationId(),
                        status = RunStatus.FAILED,
                        startedAt = startedAt,
                        model = model,
                        input = userText,
                        output = null,
                        settings = settings,
                        metrics = null,
                        error = message,
                        messages = host.state.value.messages,
                        incompleteAssistant = true,
                    ),
                )
            }
            throw error
        }
    }

    private fun rate(tokens: Int?, durationMs: Long): Double? = tokens?.let { count ->
        durationMs.takeIf { it > 0 }?.let { count * 1_000.0 / it }
    }
}
