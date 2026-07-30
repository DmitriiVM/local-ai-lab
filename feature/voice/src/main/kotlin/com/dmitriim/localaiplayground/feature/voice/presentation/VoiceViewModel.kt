package com.dmitriim.localaiplayground.feature.voice.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelLibrary
import com.dmitriim.localaiplayground.core.model.AiCapability
import com.dmitriim.localaiplayground.core.model.RunModelSnapshot
import com.dmitriim.localaiplayground.core.model.RunRecord
import com.dmitriim.localaiplayground.core.model.RunRepository
import com.dmitriim.localaiplayground.core.model.RunStatus
import com.dmitriim.localaiplayground.source.runs.RunReplayStore
import com.dmitriim.localaiplayground.core.result.ForegroundOperationCoordinator
import com.dmitriim.localaiplayground.feature.voice.domain.VoiceAssistantCoordinator
import com.dmitriim.localaiplayground.feature.voice.domain.CompletedVoiceTurn
import com.dmitriim.localaiplayground.feature.voice.domain.PersistVoiceTurn
import com.dmitriim.localaiplayground.feature.voice.domain.VoiceConversationSnapshot
import com.dmitriim.localaiplayground.ai.api.SystemSpeechToTextSupport
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

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class VoiceViewModel(
    private val modelLibrary: ModelLibrary,
    private val coordinator: VoiceAssistantCoordinator,
    private val operationCoordinator: ForegroundOperationCoordinator,
    private val runRepository: RunRepository,
    private val persistVoiceTurn: PersistVoiceTurn,
    private val replayStore: RunReplayStore,
    private val systemSpeechSupport: SystemSpeechToTextSupport,
) : ViewModel() {
    private val mutableState = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = mutableState.asStateFlow()
    private var turnJob: Job? = null
    private var conversationId = java.util.UUID.randomUUID().toString()

    init {
        viewModelScope.launch {
            modelLibrary.installedModels.collectLatest { installed ->
                mutableState.update {
                    it.withAvailableModels(
                        installed,
                        includeAndroidRecognizer = systemSpeechSupport.isOnDeviceRecognizerAvailable,
                    )
                }
            }
        }
        viewModelScope.launch {
            replayStore.pending.collectLatest { run ->
                if (run?.capability == AiCapability.VOICE_ASSISTANT) applyReplay(run)
            }
        }
    }

    fun selectSpeechModel(modelId: ModelId) = selectWhenInactive { it.copy(selectedSpeechModelId = modelId) }

    fun selectChatModel(modelId: ModelId) = selectWhenInactive { it.copy(selectedChatModelId = modelId) }

    fun selectVoiceModel(modelId: ModelId) = selectWhenInactive { it.copy(selectedVoiceModelId = modelId) }

    fun selectLanguage(language: VoiceLanguage) = selectWhenInactive { it.copy(language = language) }

    fun updateSettings(transform: (VoiceSettings) -> VoiceSettings) = selectWhenInactive {
        it.copy(settings = transform(it.settings))
    }

    fun startListening() {
        if (turnJob?.isActive == true) {
            Log.w(TAG, "Ignoring voice start request because a turn is already active.")
            return
        }
        val snapshot = mutableState.value
        val configurationError = snapshot.configurationError
        if (configurationError != null) {
            Log.w(TAG, "Voice start blocked by configuration: $configurationError")
            mutableState.update { it.copy(errorMessage = configurationError, phase = VoicePhase.ERROR) }
            return
        }
        val request = snapshot.toVoiceTurnRequest().getOrElse { error ->
            Log.e(TAG, "Voice settings are invalid: ${error.message}", error)
            mutableState.update {
                it.copy(
                    phase = VoicePhase.ERROR,
                    errorMessage = error.message ?: "Voice settings are invalid.",
                )
            }
            return
        }
        val startedAt = System.currentTimeMillis()
        Log.i(
            TAG,
            "Voice UI turn started: stt=${snapshot.selectedSpeechModel?.displayName}, chat=${snapshot.selectedChatModel?.displayName}, " +
                "tts=${snapshot.selectedVoiceModel?.displayName}, language=${request.languageCode}, " +
                "historyTurns=${request.history.size}, contextSize=${request.contextSize}, maxOutputTokens=${request.maxOutputTokens}",
        )
        mutableState.update {
            it.copy(
                phase = VoicePhase.FINALIZING,
                level = null,
                finalTranscript = "",
                streamingResponse = "",
                contextUsage = null,
                metrics = null,
                statusMessage = "Validating the selected local pipeline…",
                errorMessage = null,
            )
        }
        turnJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                var assistantTokenEvents = 0
                coordinator.execute(request).collect { event ->
                    when (event) {
                        is com.dmitriim.localaiplayground.feature.voice.domain.VoicePipelineEvent.Prepared ->
                            Log.i(TAG, "Voice UI pipeline prepared: stt=${event.pipeline.speechModel}, chat=${event.pipeline.chatModel}, tts=${event.pipeline.voiceModel}")
                        is com.dmitriim.localaiplayground.feature.voice.domain.VoicePipelineEvent.Phase ->
                            Log.i(TAG, "Voice UI phase changed: ${event.value}")
                        is com.dmitriim.localaiplayground.feature.voice.domain.VoicePipelineEvent.FinalTranscript ->
                            Log.i(TAG, "Voice UI received final transcript: length=${event.value.length}")
                        is com.dmitriim.localaiplayground.feature.voice.domain.VoicePipelineEvent.ContextPrepared ->
                            Log.i(TAG, "Voice UI context prepared: promptTokens=${event.value.promptTokens}, omittedTurns=${event.value.omittedTurnCount}")
                        is com.dmitriim.localaiplayground.feature.voice.domain.VoicePipelineEvent.AssistantToken -> {
                            assistantTokenEvents++
                            if (assistantTokenEvents == 1) Log.i(TAG, "Voice UI received first assistant token: chars=${event.value.length}")
                        }
                        is com.dmitriim.localaiplayground.feature.voice.domain.VoicePipelineEvent.AssistantCompleted ->
                            Log.i(TAG, "Voice UI assistant response completed: length=${event.value.length}, tokenEvents=$assistantTokenEvents")
                        is com.dmitriim.localaiplayground.feature.voice.domain.VoicePipelineEvent.Completed ->
                            Log.i(TAG, "Voice UI turn metrics received: listeningMs=${event.metrics.listeningDurationMs}, sttMs=${event.metrics.sttProcessingDurationMs}, llmMs=${event.metrics.llmCompletionDurationMs}, ttsMs=${event.metrics.ttsCompletionDurationMs}")
                        is com.dmitriim.localaiplayground.feature.voice.domain.VoicePipelineEvent.Level -> Unit
                    }
                    mutableState.update { it.reduce(event) }
                }
                mutableState.update { state ->
                    val user = state.finalTranscript
                    val assistant = state.streamingResponse
                    state.copy(
                        phase = VoicePhase.IDLE,
                        level = null,
                        conversation = if (user.isNotBlank() && assistant.isNotBlank()) {
                            state.conversation + VoiceConversationTurn(userText = user, assistantText = assistant)
                        } else state.conversation,
                        statusMessage = "Turn complete. Context is kept in memory for the next turn.",
                    )
                }
                completedVoiceTurn(startedAt, snapshot, request)?.let { completed ->
                    persistVoiceTurn(completed)
                    Log.i(TAG, "Voice UI completed turn persisted.")
                }
            } catch (cancelled: CancellationException) {
                Log.i(TAG, "Voice UI turn cancelled.")
                mutableState.update {
                    it.copy(phase = VoicePhase.IDLE, level = null, statusMessage = "Voice turn cancelled.")
                }
                persistTerminalTurn(RunStatus.CANCELLED, startedAt, snapshot, "Voice turn cancelled.")
            } catch (error: Throwable) {
                Log.e(TAG, "Voice UI turn failed: ${error.message}", error)
                mutableState.update {
                    it.copy(
                        phase = VoicePhase.ERROR,
                        level = null,
                        errorMessage = error.message ?: "The local voice pipeline failed.",
                        statusMessage = null,
                    )
                }
                persistTerminalTurn(RunStatus.FAILED, startedAt, snapshot, error.message ?: "The local voice pipeline failed.")
            }
        }.also(::registerForegroundCancellation)
    }

    fun stopListening() {
        if (mutableState.value.phase != VoicePhase.LISTENING) return
        Log.i(TAG, "Voice UI stop-listening requested.")
        mutableState.update { it.copy(phase = VoicePhase.FINALIZING, level = null, statusMessage = "Finalizing recorded speech…") }
        coordinator.stopListening()
    }

    fun cancel() {
        if (turnJob?.isActive != true) return
        Log.i(TAG, "Voice UI cancellation requested: phase=${mutableState.value.phase}")
        mutableState.update { it.copy(phase = VoicePhase.CANCELLING, statusMessage = "Stopping local engines and audio…") }
        coordinator.cancel()
        turnJob?.cancel()
    }

    fun newConversation() {
        if (turnJob?.isActive == true) return
        val previous = conversationId
        conversationId = java.util.UUID.randomUUID().toString()
        Log.i(TAG, "Voice UI started a new conversation.")
        mutableState.update {
            it.copy(
                phase = VoicePhase.IDLE,
                finalTranscript = "",
                streamingResponse = "",
                conversation = emptyList(),
                contextUsage = null,
                metrics = null,
                statusMessage = "Started a new in-memory conversation.",
                errorMessage = null,
            )
        }
        viewModelScope.launch(Dispatchers.IO) { runRepository.deleteConversation(previous) }
    }

    fun microphonePermissionDenied() = mutableState.update {
        Log.w(TAG, "Voice microphone permission denied.")
        it.copy(phase = VoicePhase.ERROR, errorMessage = "Microphone permission is required for push-to-talk.")
    }

    private fun selectWhenInactive(transform: (VoiceUiState) -> VoiceUiState) {
        if (turnJob?.isActive == true) {
            Log.w(TAG, "Ignoring voice configuration change while a turn is active.")
            return
        }
        mutableState.update { transform(it).copy(errorMessage = null, statusMessage = null) }
    }

    private fun registerForegroundCancellation(job: Job) {
        val registration = operationCoordinator.register(::cancel)
        job.invokeOnCompletion { registration.close() }
    }

    override fun onCleared() {
        Log.i(TAG, "Voice ViewModel cleared; cancelling active turn.")
        coordinator.cancel()
        turnJob?.cancel()
        super.onCleared()
    }

    private fun applyReplay(run: RunRecord) {
        Log.i(TAG, "Voice replay configuration received: runId=${run.id}")
        val parameters = runCatching { Json.parseToJsonElement(run.parametersJson).jsonObject }.getOrNull()
        mutableState.update { state ->
            state.copy(
                settings = state.settings.copy(
                    systemPrompt = parameters?.get("systemPrompt")?.jsonPrimitive?.content ?: state.settings.systemPrompt,
                    temperature = parameters?.get("temperature")?.jsonPrimitive?.content ?: state.settings.temperature,
                    maxOutputTokens = parameters?.get("maxOutputTokens")?.jsonPrimitive?.content ?: state.settings.maxOutputTokens,
                    contextSize = parameters?.get("contextSize")?.jsonPrimitive?.content ?: state.settings.contextSize,
                    sttThreadCount = parameters?.get("sttThreadCount")?.jsonPrimitive?.content ?: state.settings.sttThreadCount,
                    llmThreadCount = parameters?.get("llmThreadCount")?.jsonPrimitive?.content ?: state.settings.llmThreadCount,
                    ttsThreadCount = parameters?.get("ttsThreadCount")?.jsonPrimitive?.content ?: state.settings.ttsThreadCount,
                    speakerId = parameters?.get("speakerId")?.jsonPrimitive?.content ?: state.settings.speakerId,
                    speechRate = parameters?.get("speechRate")?.jsonPrimitive?.content ?: state.settings.speechRate,
                    volume = parameters?.get("volume")?.jsonPrimitive?.content ?: state.settings.volume,
                ),
                finalTranscript = run.input.orEmpty(),
                streamingResponse = run.output.orEmpty(),
                statusMessage = "Saved voice configuration restored. Record a new turn to repeat because microphone audio is session-only.",
            )
        }
        replayStore.consume(run.id)
    }

    private fun completedVoiceTurn(
        startedAt: Long,
        snapshot: VoiceUiState,
        request: com.dmitriim.localaiplayground.feature.voice.domain.VoiceTurnRequest,
    ): CompletedVoiceTurn? {
        val final = mutableState.value
        val metrics = final.metrics ?: return null
        return CompletedVoiceTurn(
            conversationId = conversationId,
            startedAtEpochMs = startedAt,
            request = request,
            transcript = final.finalTranscript,
            response = final.streamingResponse,
            conversation = final.conversation.map { VoiceConversationSnapshot(it.id, it.userText, it.assistantText) },
            metrics = metrics,
            speechModel = snapshot.selectedSpeechModel?.let { RunModelSnapshot(it.id.value, it.displayName, it.engineId.value) },
            chatModel = snapshot.selectedChatModel?.let { RunModelSnapshot(it.id.value, it.displayName, it.engineId.value) },
            voiceModel = snapshot.selectedVoiceModel?.let { RunModelSnapshot(it.id.value, it.displayName, it.engineId.value) },
        )
    }

    private suspend fun persistTerminalTurn(status: RunStatus, startedAt: Long, snapshot: VoiceUiState, error: String) {
        Log.i(TAG, "Voice terminal turn persistence requested: status=$status, transcriptLength=${mutableState.value.finalTranscript.length}, responseLength=${mutableState.value.streamingResponse.length}")
        runRepository.saveRun(
            RunRecord(
                id = java.util.UUID.randomUUID().toString(), capability = AiCapability.VOICE_ASSISTANT,
                status = status, startedAtEpochMs = startedAt, completedAtEpochMs = System.currentTimeMillis(),
                input = mutableState.value.finalTranscript.takeIf(String::isNotBlank), output = mutableState.value.streamingResponse.takeIf(String::isNotBlank),
                parametersJson = "{}", metricsJson = "{}", errorMessage = error,
            ),
        )
    }

    private companion object {
        const val TAG = "AiP123Voice"
    }
}
