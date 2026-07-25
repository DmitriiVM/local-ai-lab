package com.dmitriim.localaiplayground.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localaiplayground.ai.api.ChatEngine
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.InstalledModel
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelRepository
import com.dmitriim.localaiplayground.core.model.ModelValidationState
import com.dmitriim.localaiplayground.core.result.ForegroundOperationCoordinator
import com.dmitriim.localaiplayground.feature.chat.domain.ChatGenerationEvent
import com.dmitriim.localaiplayground.feature.chat.domain.ChatGenerationRequest
import com.dmitriim.localaiplayground.feature.chat.domain.GenerateChatResponse
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class ChatViewModel(
    private val modelRepository: ModelRepository,
    private val chatEngine: ChatEngine,
    private val generateChatResponse: GenerateChatResponse,
    private val operationCoordinator: ForegroundOperationCoordinator,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = mutableState.asStateFlow()
    private var generationJob: Job? = null

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
        mutableState.update { it.copy(messages = emptyList(), metrics = null, contextUsage = null, errorMessage = null) }
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
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                mutableState.update { it.copy(operation = ChatOperation.IDLE, errorMessage = "Generation cancelled.") }
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
}

private fun rate(tokens: Int, durationMs: Long): Double? = durationMs.takeIf { it > 0 }?.let { tokens * 1_000.0 / it }
