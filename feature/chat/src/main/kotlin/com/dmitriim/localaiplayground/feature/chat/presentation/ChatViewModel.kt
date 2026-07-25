package com.dmitriim.localaiplayground.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localaiplayground.ai.api.ChatEngine
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.ConversationMessageRole
import com.dmitriim.localaiplayground.core.model.InstalledModel
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelRepository
import com.dmitriim.localaiplayground.core.model.RunModelSnapshot
import com.dmitriim.localaiplayground.core.model.RunRecord
import com.dmitriim.localaiplayground.core.model.RunRepository
import com.dmitriim.localaiplayground.core.model.RunStatus
import com.dmitriim.localaiplayground.core.result.ForegroundOperationCoordinator
import com.dmitriim.localaiplayground.feature.chat.domain.ChatGenerationEvent
import com.dmitriim.localaiplayground.feature.chat.domain.ChatGenerationRequest
import com.dmitriim.localaiplayground.feature.chat.domain.GenerateChatResponse
import com.dmitriim.localaiplayground.feature.chat.domain.PersistChatTurn
import com.dmitriim.localaiplayground.feature.chat.domain.ChatConversationSnapshot
import com.dmitriim.localaiplayground.feature.chat.domain.ChatPersistenceSnapshot
import com.dmitriim.localaiplayground.feature.chat.domain.ChatRunMetrics
import com.dmitriim.localaiplayground.feature.chat.domain.ChatRunSettings
import com.dmitriim.localaiplayground.source.runs.RunReplayStore
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class ChatViewModel(
    private val modelRepository: ModelRepository,
    private val chatEngine: ChatEngine,
    private val generateChatResponse: GenerateChatResponse,
    private val operationCoordinator: ForegroundOperationCoordinator,
    private val runRepository: RunRepository,
    private val persistChatTurn: PersistChatTurn,
    private val replayStore: RunReplayStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = mutableState.asStateFlow()
    private var generationJob: Job? = null
    private var conversationId = UUID.randomUUID().toString()

    init {
        viewModelScope.launch {
            modelRepository.installedModels.collectLatest { installed ->
                val models = installed.filter(::isReadyChatModel).map(InstalledModel::toChatModelOption)
                mutableState.update { current ->
                    val selected = current.selectedModelId?.takeIf { id -> models.any { it.id == id } }
                        ?: models.firstOrNull()?.id
                    val selectedModel = models.firstOrNull { it.id == selected }
                    current.copy(
                        availableModels = models,
                        selectedModelId = selected,
                        settings = if (current.selectedModelId != selected && selectedModel != null) {
                            current.settings.copy(contextSize = selectedModel.defaultContextSize.toString())
                        } else {
                            current.settings
                        },
                    )
                }
            }
        }
        viewModelScope.launch {
            replayStore.pending.collectLatest { run ->
                if (run?.capability == com.dmitriim.localaiplayground.core.model.AiCapability.CHAT) applyReplay(run)
            }
        }
    }

    fun selectModel(modelId: ModelId) {
        if (generationJob?.isActive == true) return
        val model = mutableState.value.availableModels.firstOrNull { it.id == modelId } ?: return
        mutableState.update {
            it.copy(
                selectedModelId = modelId,
                settings = it.settings.copy(contextSize = model.defaultContextSize.toString()),
                metrics = null,
                errorMessage = null,
            )
        }
    }

    fun updateInput(value: String) = mutableState.update { it.copy(input = value) }

    fun updateSettings(transform: (ChatSettings) -> ChatSettings) = mutableState.update {
        it.copy(settings = transform(it.settings), errorMessage = null)
    }

    fun resetSettings() = mutableState.update {
        val context = it.availableModels.firstOrNull { model -> model.id == it.selectedModelId }?.defaultContextSize ?: 512
        it.copy(settings = ChatSettings(contextSize = context.toString()))
    }

    fun send() {
        val snapshot = mutableState.value
        val input = snapshot.input.trim()
        if (input.isEmpty() || generationJob?.isActive == true) return
        startGeneration(snapshot.messages, input, appendUser = true)
    }

    fun stop() {
        if (generationJob?.isActive != true) return
        mutableState.update { it.copy(operation = ChatOperation.CANCELLING) }
        chatEngine.cancel()
    }

    fun regenerate() {
        val messages = mutableState.value.messages
        val assistantIndex = messages.indexOfLast { it.role == ChatMessageRole.ASSISTANT }
        if (assistantIndex < 0 || generationJob?.isActive == true) return
        val user = messages.subList(0, assistantIndex).lastOrNull { it.role == ChatMessageRole.USER } ?: return
        startGeneration(messages.take(assistantIndex), user.content, appendUser = false)
    }

    /** Makes a user message editable and removes the dependent response branch. */
    fun editAndRetry(messageId: String) {
        if (generationJob?.isActive == true) return
        val messages = mutableState.value.messages
        val index = messages.indexOfFirst { it.id == messageId && it.role == ChatMessageRole.USER }
        if (index < 0) return
        mutableState.update {
            it.copy(messages = messages.take(index), input = messages[index].content, metrics = null, errorMessage = null)
        }
    }

    fun clearConversation() {
        if (generationJob?.isActive == true) return
        val deleted = conversationId
        conversationId = UUID.randomUUID().toString()
        mutableState.update { it.copy(messages = emptyList(), metrics = null, contextUsage = null, errorMessage = null) }
        viewModelScope.launch(Dispatchers.IO) { runRepository.deleteConversation(deleted) }
    }

    fun unloadModel() {
        if (generationJob?.isActive == true) return
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { chatEngine.unload() }
                .onSuccess { mutableState.update { it.copy(errorMessage = null) } }
                .onFailure { error -> mutableState.update { it.copy(errorMessage = error.message ?: "Could not unload the model.") } }
        }
    }

    private fun startGeneration(base: List<ChatMessage>, userText: String, appendUser: Boolean) {
        val snapshot = mutableState.value
        val selectedId = snapshot.selectedModelId
        if (selectedId == null) {
            mutableState.update { it.copy(errorMessage = "Select an installed, compatible GGUF chat model first.") }
            return
        }
        val settings = runCatching { snapshot.settings.toEffective() }.getOrElse { error ->
            mutableState.update { it.copy(errorMessage = error.message ?: "Generation settings are invalid.") }
            return
        }
        val visibleMessages = if (appendUser) base + ChatMessage.user(userText) else base
        val startedAt = System.currentTimeMillis()
        val model = snapshot.availableModels.firstOrNull { it.id == selectedId }
        mutableState.update {
            it.copy(
                input = if (appendUser) "" else it.input,
                messages = visibleMessages,
                operation = ChatOperation.LOADING,
                errorMessage = null,
                metrics = null,
            )
        }
        val job = viewModelScope.launch(Dispatchers.Default) {
            try {
                var assistantId: String? = null
                generateChatResponse.execute(
                    ChatGenerationRequest(
                        modelId = selectedId,
                        turns = visibleMessages.map(ChatMessage::toDomain),
                        config = settings.toDomain(),
                    ),
                ).collect { event ->
                    when (event) {
                        is ChatGenerationEvent.Prepared -> {
                            assistantId = UUID.randomUUID().toString()
                            mutableState.update {
                                it.copy(
                                    operation = ChatOperation.GENERATING,
                                    contextUsage = event.contextUsage.toUi(),
                                    messages = visibleMessages + ChatMessage.assistant(assistantId.orEmpty(), "", streaming = true),
                                )
                            }
                        }
                        is ChatGenerationEvent.Token -> {
                            assistantId?.let { id ->
                                mutableState.update { state -> state.replaceAssistantText(id, event.text, append = true) }
                            }
                        }
                        is ChatGenerationEvent.Completed -> {
                            val id = requireNotNull(assistantId) { "The local engine completed without preparing a response." }
                            val result = event.generation
                            val metrics = ChatMetrics(
                                modelName = event.modelName,
                                coldStart = event.load.coldStart,
                                loadDurationMs = event.load.loadDurationMs,
                                promptTokens = result.promptTokenCount,
                                promptTokensPerSecond = rate(result.promptTokenCount, result.promptDurationMs),
                                timeToFirstTokenMs = result.firstTokenLatencyMs,
                                generatedTokens = result.generatedTokenCount,
                                generatedTokensPerSecond = rate(result.generatedTokenCount, result.generationDurationMs),
                                totalDurationMs = result.totalDurationMs,
                                finishReason = result.finishReason,
                                effectiveSettings = settings,
                                effectiveThreadCount = event.load.effectiveThreadCount,
                            )
                            mutableState.update { state ->
                                state.replaceAssistantText(id, result.text, append = false).copy(
                                    operation = ChatOperation.IDLE,
                                    metrics = metrics,
                                )
                            }
                            persistChatTurn(
                                snapshotForPersistence(
                                    status = RunStatus.SUCCEEDED,
                                    startedAt = startedAt,
                                    model = model,
                                    input = userText,
                                    output = result.text,
                                    settings = settings,
                                    metrics = metrics,
                                    error = null,
                                    messages = mutableState.value.messages,
                                ),
                            )
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                mutableState.update { it.copy(operation = ChatOperation.IDLE, errorMessage = "Generation cancelled.") }
                val partial = mutableState.value.messages.lastOrNull { it.role == ChatMessageRole.ASSISTANT }?.content
                persistChatTurn(snapshotForPersistence(RunStatus.CANCELLED, startedAt, model, userText, partial, settings, null, "Generation cancelled.", mutableState.value.messages, true))
            } catch (error: Throwable) {
                mutableState.update { state ->
                    state.copy(
                        operation = ChatOperation.IDLE,
                        errorMessage = error.message ?: "Local generation failed.",
                        messages = state.messages.map { message ->
                            if (message.streaming) message.copy(streaming = false, failed = true) else message
                        },
                    )
                }
                persistChatTurn(snapshotForPersistence(RunStatus.FAILED, startedAt, model, userText, null, settings, null, error.message ?: "Local generation failed.", mutableState.value.messages, true))
            }
        }
        generationJob = job
        val registration = operationCoordinator.register(chatEngine::cancel)
        job.invokeOnCompletion { registration.close() }
    }

    override fun onCleared() {
        chatEngine.cancel()
        super.onCleared()
    }

    private fun applyReplay(run: RunRecord) {
        val modelId = run.model?.modelId?.let(::ModelId)
        val available = mutableState.value.availableModels
        val selected = modelId?.takeIf { candidate -> available.any { it.id == candidate } }
        val parameters = runCatching { Json.parseToJsonElement(run.parametersJson).jsonObject }.getOrNull()
        mutableState.update { state ->
            state.copy(
                selectedModelId = selected ?: state.selectedModelId,
                input = run.input.orEmpty(),
                settings = state.settings.copy(
                    systemPrompt = parameters?.get("systemPrompt")?.jsonPrimitive?.content ?: state.settings.systemPrompt,
                    temperature = parameters?.get("temperature")?.jsonPrimitive?.content ?: state.settings.temperature,
                    topK = parameters?.get("topK")?.jsonPrimitive?.content ?: state.settings.topK,
                    topP = parameters?.get("topP")?.jsonPrimitive?.content ?: state.settings.topP,
                    maxOutputTokens = parameters?.get("maxOutputTokens")?.jsonPrimitive?.content ?: state.settings.maxOutputTokens,
                    seed = parameters?.get("seed")?.jsonPrimitive?.content ?: state.settings.seed,
                    contextSize = parameters?.get("contextSize")?.jsonPrimitive?.content ?: state.settings.contextSize,
                    threadCount = parameters?.get("threadCount")?.jsonPrimitive?.content ?: state.settings.threadCount,
                ),
                errorMessage = if (modelId != null && selected == null) "Saved model ${run.model?.displayName.orEmpty()} is no longer installed. Select a compatible model before running." else null,
            )
        }
        replayStore.consume(run.id)
    }

    private fun snapshotForPersistence(
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
        conversationId = conversationId,
        status = status,
        startedAtEpochMs = startedAt,
        model = model?.let { RunModelSnapshot(it.id.value, it.displayName, "llama.cpp") },
        input = input,
        output = output,
        settings = ChatRunSettings(
            settings.systemPrompt, settings.temperature, settings.topK, settings.topP,
            settings.maxOutputTokens, settings.seed, settings.contextSize, settings.threadCount,
        ),
        metrics = metrics?.let {
            ChatRunMetrics(it.coldStart, it.loadDurationMs, it.promptTokens, it.timeToFirstTokenMs, it.generatedTokens, it.totalDurationMs, it.finishReason.name, it.effectiveThreadCount)
        },
        errorMessage = error,
        messages = messages.map { message ->
            ChatConversationSnapshot(
                id = message.id,
                role = if (message.role == ChatMessageRole.USER) ConversationMessageRole.USER else ConversationMessageRole.ASSISTANT,
                content = message.content,
                incomplete = message.streaming || (incompleteAssistant && message.role == ChatMessageRole.ASSISTANT && message.failed),
            )
        },
    )
}

private fun rate(tokens: Int, durationMs: Long): Double? = durationMs.takeIf { it > 0 }?.let { tokens * 1_000.0 / it }
