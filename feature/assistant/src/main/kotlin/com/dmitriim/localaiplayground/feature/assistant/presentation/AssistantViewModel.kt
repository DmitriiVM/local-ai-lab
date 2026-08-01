package com.dmitriim.localaiplayground.feature.assistant.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localaiplayground.ai.api.llm.ChatEngine
import com.dmitriim.localaiplayground.ai.api.system.SystemSpeechToTextSupport
import com.dmitriim.localaiplayground.ai.api.system.SystemTextToSpeechSupport
import com.dmitriim.localaiplayground.core.audio.input.model.PcmAudioInput
import com.dmitriim.localaiplayground.core.audio.input.storage.ReferenceVoiceStore
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.conversation.ConversationMessageRole
import com.dmitriim.localaiplayground.core.model.engine.ComputePreference
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.model.runs.RunModelSnapshot
import com.dmitriim.localaiplayground.core.model.runs.RunRecord
import com.dmitriim.localaiplayground.core.model.runs.RunStatus
import com.dmitriim.localaiplayground.core.model.service.ModelLibrary
import com.dmitriim.localaiplayground.core.model.service.ModelTransfers
import com.dmitriim.localaiplayground.core.model.service.RunRepository
import com.dmitriim.localaiplayground.core.result.ForegroundOperationCoordinator
import com.dmitriim.localaiplayground.feature.assistant.domain.AssistantAudioRecorder
import com.dmitriim.localaiplayground.feature.assistant.domain.AssistantConversationSnapshot
import com.dmitriim.localaiplayground.feature.assistant.domain.AssistantRunRecorder
import com.dmitriim.localaiplayground.feature.assistant.domain.AssistantSpeechOutput
import com.dmitriim.localaiplayground.feature.assistant.domain.AssistantTranscriber
import com.dmitriim.localaiplayground.feature.assistant.domain.ChatGenerationEvent
import com.dmitriim.localaiplayground.feature.assistant.domain.ChatGenerationRequest
import com.dmitriim.localaiplayground.feature.assistant.domain.ChatPersistenceSnapshot
import com.dmitriim.localaiplayground.feature.assistant.domain.ChatRunMetrics
import com.dmitriim.localaiplayground.feature.assistant.domain.ChatRunSettings
import com.dmitriim.localaiplayground.feature.assistant.domain.GenerateAssistantResponse
import com.dmitriim.localaiplayground.feature.assistant.domain.PersistAssistantTurn
import com.dmitriim.localaiplayground.feature.stt.domain.SpeechTranscriptionEvent
import com.dmitriim.localaiplayground.feature.stt.domain.SpeechTranscriptionMetrics
import com.dmitriim.localaiplayground.feature.tts.domain.SpeechSynthesisEvent
import com.dmitriim.localaiplayground.feature.tts.domain.SpeechSynthesisMetrics
import com.dmitriim.localaiplayground.source.runs.RunReplayStore
import com.dmitriim.localaiplayground.source.settings.AssistantChatPreferences
import com.dmitriim.localaiplayground.source.settings.AssistantPreferences
import com.dmitriim.localaiplayground.source.settings.AssistantPreferencesRepository
import com.dmitriim.localaiplayground.source.settings.AssistantSpeechInputPreferences
import com.dmitriim.localaiplayground.source.settings.AssistantSpeechOutputPreferences
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class AssistantViewModel(
    private val modelLibrary: ModelLibrary,
    private val modelTransfers: ModelTransfers,
    private val systemSpeechSupport: SystemSpeechToTextSupport,
    private val systemTextToSpeechSupport: SystemTextToSpeechSupport,
    private val referenceVoiceStore: ReferenceVoiceStore,
    private val chatEngine: ChatEngine,
    private val generateResponse: GenerateAssistantResponse,
    private val audioRecorder: AssistantAudioRecorder,
    private val transcriber: AssistantTranscriber,
    private val speechOutput: AssistantSpeechOutput,
    private val runRecorder: AssistantRunRecorder,
    private val persistAssistantTurn: PersistAssistantTurn,
    private val preferencesRepository: AssistantPreferencesRepository,
    private val operationCoordinator: ForegroundOperationCoordinator,
    private val runRepository: RunRepository,
    private val replayStore: RunReplayStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AssistantUiState())
    val state: StateFlow<AssistantUiState> = mutableState.asStateFlow()

    private var activeJob: Job? = null
    private var conversationId = UUID.randomUUID().toString()
    private val activeLinkedRunIds = mutableListOf<String>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching(systemTextToSpeechSupport::refresh)
                .onFailure { Log.w(TAG, "Android TTS discovery failed: ${it.message}") }
        }
        viewModelScope.launch {
            val options = combine(
                modelLibrary.installedModels,
                modelTransfers.catalog,
                referenceVoiceStore.voices,
                systemTextToSpeechSupport.voices,
            ) { installed, catalog, references, systemVoices ->
                AssistantModelOptions(
                    chat = chatModelOptions(installed, catalog, chatEngine::capabilitiesFor),
                    speech = speechModelOptions(
                        installed,
                        catalog,
                        includeAndroidRecognizer = systemSpeechSupport.isOnDeviceRecognizerAvailable,
                    ),
                    voice = textToSpeechModelOptions(installed, catalog, references, systemVoices),
                )
            }
            combine(options, preferencesRepository.preferences, ::Pair).collectLatest { (modelOptions, preferences) ->
                val reconciled = mutableState.value.withConfiguration(modelOptions, preferences)
                mutableState.value = reconciled
                val effectivePreferences = reconciled.toPreferences()
                if (effectivePreferences != preferences) {
                    preferencesRepository.update(effectivePreferences)
                }
            }
        }
        viewModelScope.launch {
            replayStore.pending.collectLatest { run ->
                if (run?.capability in setOf(AiCapability.CHAT, AiCapability.VOICE_ASSISTANT)) applyReplay(run!!)
            }
        }
    }

    fun updateInput(value: String) = mutableState.update {
        it.copy(input = value, errorMessage = null, statusMessage = null)
    }

    fun selectInputMode(mode: AssistantInputMode) {
        if (!mutableState.value.isIdle) return
        if (mode == AssistantInputMode.VOICE) {
            mutableState.value.voiceConfigurationError?.let { error ->
                mutableState.update { it.copy(errorMessage = error) }
                return
            }
        }
        mutableState.update { it.copy(inputMode = mode, errorMessage = null) }
    }

    fun applyChatSettings(modelId: ModelId, settings: ChatSettings): String? {
        if (!mutableState.value.isIdle) return "Wait for the current operation to finish."
        val model = mutableState.value.chatModels.firstOrNull { it.id == modelId && it.installed }
            ?: return "Select an installed chat model."
        if (model.capabilities == null) return "The selected model's LLM runtime is not packaged."
        val normalizedSettings = settings.copy(
            computePreference = model.supportedComputePreference(settings.computePreference),
        )
        val error = runCatching(normalizedSettings::toEffective).exceptionOrNull()?.message
        if (error != null) return error
        val modelChanged = mutableState.value.selectedChatModelId != modelId
        mutableState.update {
            it.copy(
                selectedChatModelId = modelId,
                chatSettings = normalizedSettings,
                metrics = null,
                errorMessage = null,
                statusMessage = if (modelChanged) "${model.displayName} will be used for the next message." else null,
            )
        }
        if (modelChanged) viewModelScope.launch(Dispatchers.Default) { chatEngine.unload() }
        persistPreferences()
        return null
    }

    fun applySpeechInputSettings(modelId: ModelId, settings: SpeechInputSettings): String? {
        if (!mutableState.value.isIdle) return "Wait for the current operation to finish."
        val model = mutableState.value.speechModels.firstOrNull { it.id == modelId && it.installed }
            ?: return "Select an installed speech-to-text model."
        val error = runCatching(settings::validate).exceptionOrNull()?.message
        if (error != null) return error
        if (!model.supports(settings.languageCode)) return "${model.displayName} does not support this language."
        mutableState.update {
            it.copy(selectedSpeechModelId = modelId, speechInputSettings = settings, errorMessage = null)
        }
        persistPreferences()
        return null
    }

    fun applySpeechOutputSettings(
        modelId: ModelId,
        voiceId: String,
        settings: SpeechOutputSettings,
    ): String? {
        if (!mutableState.value.isIdle) return "Wait for the current operation to finish."
        val model = mutableState.value.voiceModels.firstOrNull { it.id == modelId && it.installed }
            ?: return "Select an installed text-to-speech model."
        val error = runCatching(settings::validate).exceptionOrNull()?.message
        if (error != null) return error
        if (model.compatibleVoices(settings.languageCode).none { it.id == voiceId }) {
            return "Select a voice compatible with this language."
        }
        val modelChanged = mutableState.value.selectedVoiceModelId != modelId
        mutableState.update {
            it.copy(
                selectedVoiceModelId = modelId,
                selectedVoiceId = voiceId,
                speechOutputSettings = settings,
                errorMessage = null,
            )
        }
        if (modelChanged) viewModelScope.launch(Dispatchers.Default) { speechOutput.unload() }
        persistPreferences()
        return null
    }

    fun unloadChatModel() {
        if (!mutableState.value.isIdle) return
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { chatEngine.unload() }
                .onSuccess { mutableState.update { it.copy(statusMessage = "Chat model unloaded.") } }
                .onFailure { error -> mutableState.update { it.copy(errorMessage = error.message ?: "Could not unload the chat model.") } }
        }
    }

    fun send() {
        val snapshot = mutableState.value
        val input = snapshot.input.trim()
        if (input.isEmpty() || !snapshot.isIdle) return
        launchForeground {
            runCatching {
                generateInternal(
                    base = snapshot.messages,
                    userText = input,
                    appendUser = true,
                    speakAfter = snapshot.inputMode == AssistantInputMode.VOICE,
                )
            }.onFailure(::handleOperationFailure)
        }
    }

    fun startRecording() {
        val snapshot = mutableState.value
        if (!snapshot.isIdle) return
        val speechModel = snapshot.selectedSpeechModel?.takeIf { it.installed }
        if (speechModel == null) {
            mutableState.update { it.copy(errorMessage = "Configure speech-to-text before using the microphone.") }
            return
        }
        if (snapshot.inputMode == AssistantInputMode.VOICE) {
            snapshot.voiceConfigurationError?.let { error ->
                mutableState.update { it.copy(errorMessage = error) }
                return
            }
        }
        val settingsError = runCatching(snapshot.speechInputSettings::validate).exceptionOrNull()?.message
        if (settingsError != null) {
            mutableState.update { it.copy(errorMessage = settingsError) }
            return
        }
        launchForeground { recordAndTranscribe(snapshot, speechModel) }
    }

    fun stopRecording() {
        if (mutableState.value.operation != AssistantOperation.Recording) return
        mutableState.update {
            it.copy(operation = AssistantOperation.Transcribing, level = null, statusMessage = "Finalizing recorded speech…")
        }
        audioRecorder.stop()
    }

    fun microphonePermissionDenied() = mutableState.update {
        it.copy(errorMessage = "Microphone permission is required for voice input.")
    }

    fun speakMessage(messageId: String) {
        val snapshot = mutableState.value
        if (!snapshot.isIdle) return
        val message = snapshot.messages.firstOrNull {
            it.id == messageId && it.role == ChatMessageRole.ASSISTANT && !it.streaming && it.content.isNotBlank()
        } ?: return
        launchForeground {
            val outcome = speakInternal(message.content, message.id)
            if (!outcome.succeeded && outcome.error != null) {
                mutableState.update { it.copy(errorMessage = outcome.error) }
            }
        }
    }

    fun previewVoice(
        modelId: ModelId,
        voiceId: String,
        settings: SpeechOutputSettings,
    ): String? {
        val snapshot = mutableState.value
        if (!snapshot.isIdle) return "Wait for the current operation to finish."
        val model = snapshot.voiceModels.firstOrNull { it.id == modelId && it.installed }
            ?: return "Select an installed text-to-speech model."
        val validationError = runCatching(settings::validate).exceptionOrNull()?.message
        if (validationError != null) return validationError
        val voice = model.compatibleVoices(settings.languageCode).firstOrNull { it.id == voiceId }
            ?: return "Select a voice compatible with this language."
        launchForeground {
            try {
                mutableState.update { it.copy(operation = AssistantOperation.Speaking, statusMessage = "Previewing ${voice.displayName}…", errorMessage = null) }
                speechOutput.preview(model.id, model, voice, snapshot.speechOutputSettings)
                mutableState.update { it.copy(operation = AssistantOperation.Idle, statusMessage = "Voice preview completed.") }
            } catch (cancelled: CancellationException) {
                mutableState.update { it.copy(operation = AssistantOperation.Idle, statusMessage = "Voice preview stopped.") }
            } catch (error: Throwable) {
                mutableState.update { it.copy(operation = AssistantOperation.Idle, errorMessage = error.message ?: "Could not preview this voice.") }
            }
        }
        return null
    }

    fun regenerate() {
        val snapshot = mutableState.value
        val assistantIndex = snapshot.messages.indexOfLast { it.role == ChatMessageRole.ASSISTANT }
        if (assistantIndex < 0 || !snapshot.isIdle) return
        val user = snapshot.messages.subList(0, assistantIndex).lastOrNull { it.role == ChatMessageRole.USER } ?: return
        launchForeground {
            runCatching {
                generateInternal(
                    base = snapshot.messages.take(assistantIndex),
                    userText = user.content,
                    appendUser = false,
                    speakAfter = snapshot.inputMode == AssistantInputMode.VOICE,
                )
            }.onFailure(::handleOperationFailure)
        }
    }

    fun editAndRetry(messageId: String) {
        val snapshot = mutableState.value
        if (!snapshot.isIdle) return
        val index = snapshot.messages.indexOfFirst { it.id == messageId && it.role == ChatMessageRole.USER }
        if (index < 0) return
        mutableState.update {
            it.copy(messages = snapshot.messages.take(index), input = snapshot.messages[index].content, metrics = null, errorMessage = null)
        }
    }

    fun clearConversation() {
        if (!mutableState.value.isIdle) return
        val oldId = conversationId
        conversationId = UUID.randomUUID().toString()
        mutableState.update {
            it.copy(messages = emptyList(), input = "", metrics = null, contextUsage = null, statusMessage = null, errorMessage = null)
        }
        viewModelScope.launch(Dispatchers.IO) { runRepository.deleteConversation(oldId) }
    }

    fun cancel() {
        if (activeJob?.isActive != true) return
        mutableState.update { it.copy(operation = AssistantOperation.Cancelling, statusMessage = "Stopping…") }
        audioRecorder.stop()
        transcriber.cancel()
        chatEngine.cancel()
        speechOutput.cancel()
        activeJob?.cancel(CancellationException("Assistant operation cancelled"))
    }

    private suspend fun recordAndTranscribe(
        initial: AssistantUiState,
        speechModel: SpeechModelOption,
    ) {
        val startedAt = System.currentTimeMillis()
        var input: PcmAudioInput? = null
        var transcript: String? = null
        var metrics: SpeechTranscriptionMetrics? = null
        activeLinkedRunIds.clear()
        try {
            mutableState.update {
                it.copy(operation = AssistantOperation.Recording, level = null, statusMessage = "Listening…", errorMessage = null)
            }
            input = audioRecorder.record(speechModel.sampleRateHz) { level ->
                mutableState.update { state -> state.copy(level = level) }
            }
            mutableState.update {
                it.copy(operation = AssistantOperation.Transcribing, level = null, statusMessage = "Transcribing locally…")
            }
            transcriber.transcribe(
                modelId = speechModel.id,
                input = input,
                languageCode = initial.speechInputSettings.languageCode,
                threadCount = initial.speechInputSettings.threadCount,
            ).collect { event ->
                if (event is SpeechTranscriptionEvent.Completed) {
                    transcript = event.transcript.trim()
                    metrics = event.metrics
                }
            }
            require(!transcript.isNullOrBlank()) { "No speech was recognized. Record another turn and try again." }
            val sttRunId = runRecorder.recordSpeechInput(
                status = RunStatus.SUCCEEDED,
                startedAtEpochMs = startedAt,
                model = speechModel.toRunSnapshot(),
                transcript = transcript,
                languageCode = initial.speechInputSettings.languageCode,
                threadCount = initial.speechInputSettings.threadCount.toInt(),
                metrics = metrics,
                error = null,
            )
            activeLinkedRunIds += sttRunId
            if (initial.inputMode == AssistantInputMode.DICTATE) {
                mutableState.update { state ->
                    state.copy(
                        input = appendTranscript(initial.input, requireNotNull(transcript)),
                        operation = AssistantOperation.Idle,
                        statusMessage = "Transcript added to the message draft.",
                    )
                }
            } else {
                val response = generateInternal(
                    base = initial.messages,
                    userText = requireNotNull(transcript),
                    appendUser = true,
                    speakAfter = true,
                )
                runRecorder.recordVoiceTurn(
                    status = if (response.speechSucceeded) RunStatus.SUCCEEDED else RunStatus.FAILED,
                    startedAtEpochMs = startedAt,
                    transcript = transcript,
                    response = response.text,
                    linkedRunIds = activeLinkedRunIds.toList(),
                    error = response.speechError,
                )
            }
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                if (activeLinkedRunIds.isEmpty()) {
                    activeLinkedRunIds += runRecorder.recordSpeechInput(
                        RunStatus.CANCELLED,
                        startedAt,
                        speechModel.toRunSnapshot(),
                        transcript,
                        initial.speechInputSettings.languageCode,
                        initial.speechInputSettings.threadCount.toIntOrNull() ?: 0,
                        metrics,
                        "Voice input cancelled.",
                    )
                }
                if (initial.inputMode == AssistantInputMode.VOICE) {
                    runRecorder.recordVoiceTurn(
                        RunStatus.CANCELLED,
                        startedAt,
                        transcript,
                        null,
                        activeLinkedRunIds.toList(),
                        "Voice turn cancelled.",
                    )
                }
            }
            mutableState.update {
                it.copy(operation = AssistantOperation.Idle, level = null, speakingMessageId = null, statusMessage = "Voice operation stopped.")
            }
        } catch (error: Throwable) {
            withContext(NonCancellable) {
                if (activeLinkedRunIds.isEmpty()) {
                    activeLinkedRunIds += runRecorder.recordSpeechInput(
                        RunStatus.FAILED,
                        startedAt,
                        speechModel.toRunSnapshot(),
                        transcript,
                        initial.speechInputSettings.languageCode,
                        initial.speechInputSettings.threadCount.toIntOrNull() ?: 0,
                        metrics,
                        error.message,
                    )
                }
                if (initial.inputMode == AssistantInputMode.VOICE) {
                    runRecorder.recordVoiceTurn(
                        RunStatus.FAILED,
                        startedAt,
                        transcript,
                        null,
                        activeLinkedRunIds.toList(),
                        error.message,
                    )
                }
            }
            handleOperationFailure(error)
        } finally {
            audioRecorder.clear(input)
        }
    }

    private suspend fun generateInternal(
        base: List<ChatMessage>,
        userText: String,
        appendUser: Boolean,
        speakAfter: Boolean,
    ): GenerationOutcome {
        val snapshot = mutableState.value
        val selectedId = snapshot.selectedChatModelId ?: error("Select an installed chat model first.")
        val settings = snapshot.chatSettings.toEffective()
        val visibleMessages = if (appendUser) base + ChatMessage.user(userText) else base
        val startedAt = System.currentTimeMillis()
        val model = snapshot.chatModels.firstOrNull { it.id == selectedId }
        var assistantId: String? = null
        try {
            mutableState.update {
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
                ChatGenerationRequest(selectedId, visibleMessages.map(ChatMessage::toDomain), settings.toDomain()),
            ).collect { event ->
                when (event) {
                    is ChatGenerationEvent.Prepared -> {
                        assistantId = UUID.randomUUID().toString()
                        mutableState.update {
                            it.copy(
                                operation = AssistantOperation.Generating,
                                statusMessage = "Generating locally…",
                                contextUsage = event.contextUsage.toUi(),
                                messages = visibleMessages + ChatMessage.assistant(requireNotNull(assistantId), "", true),
                            )
                        }
                    }
                    is ChatGenerationEvent.Token -> assistantId?.let { id ->
                        mutableState.update { it.replaceAssistantText(id, event.text, append = true) }
                    }
                    is ChatGenerationEvent.Completed -> {
                        val id = requireNotNull(assistantId) { "The chat engine completed without preparing a response." }
                        val result = event.generation
                        completedText = result.text
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
                            effectiveThreadCount = event.load.diagnostics.effectiveThreadCount,
                        )
                        mutableState.update { state ->
                            state.replaceAssistantText(id, result.text, append = false).copy(
                                operation = AssistantOperation.Idle,
                                metrics = metrics,
                                statusMessage = null,
                            )
                        }
                        val chatRunId = persistAssistantTurn(
                            snapshotForPersistence(
                                RunStatus.SUCCEEDED,
                                startedAt,
                                model,
                                userText,
                                result.text,
                                settings,
                                metrics,
                                null,
                                mutableState.value.messages,
                            ),
                        )
                        activeLinkedRunIds += chatRunId
                    }
                }
            }
            val speech = if (speakAfter) speakInternal(completedText, requireNotNull(assistantId)) else SpeechOutcome()
            return GenerationOutcome(completedText, speech.succeeded, speech.error)
        } catch (cancelled: CancellationException) {
            mutableState.update { state ->
                state.copy(
                    operation = AssistantOperation.Idle,
                    speakingMessageId = null,
                    statusMessage = "Generation stopped.",
                    messages = state.messages.map { if (it.streaming) it.copy(streaming = false, failed = true) else it },
                )
            }
            val partial = mutableState.value.messages.lastOrNull { it.role == ChatMessageRole.ASSISTANT }?.content
            withContext(NonCancellable) {
                activeLinkedRunIds += persistAssistantTurn(
                    snapshotForPersistence(
                        RunStatus.CANCELLED,
                        startedAt,
                        model,
                        userText,
                        partial,
                        settings,
                        null,
                        "Generation cancelled.",
                        mutableState.value.messages,
                        true,
                    ),
                )
            }
            throw cancelled
        } catch (error: Throwable) {
            mutableState.update { state ->
                state.copy(
                    operation = AssistantOperation.Idle,
                    speakingMessageId = null,
                    errorMessage = error.message ?: "Local generation failed.",
                    messages = state.messages.map { if (it.streaming) it.copy(streaming = false, failed = true) else it },
                )
            }
            withContext(NonCancellable) {
                activeLinkedRunIds += persistAssistantTurn(
                    snapshotForPersistence(
                        RunStatus.FAILED,
                        startedAt,
                        model,
                        userText,
                        null,
                        settings,
                        null,
                        error.message ?: "Local generation failed.",
                        mutableState.value.messages,
                        true,
                    ),
                )
            }
            throw error
        }
    }

    private suspend fun speakInternal(text: String, messageId: String): SpeechOutcome {
        val snapshot = mutableState.value
        val model = snapshot.selectedVoiceModel
        val voice = snapshot.selectedVoice
        if (model == null || voice == null || !model.installed) {
            return SpeechOutcome(succeeded = false, error = "Configure text-to-speech before speaking responses.")
        }
        val startedAt = System.currentTimeMillis()
        var metrics: SpeechSynthesisMetrics? = null
        return try {
            mutableState.update {
                it.copy(
                    operation = AssistantOperation.Speaking,
                    speakingMessageId = messageId,
                    statusMessage = "Speaking with ${voice.displayName}…",
                    errorMessage = null,
                )
            }
            speechOutput.speak(model.id, model, voice, text, snapshot.speechOutputSettings).collect { event ->
                if (event is SpeechSynthesisEvent.Completed) metrics = event.metrics
            }
            val runId = runRecorder.recordSpeechOutput(
                RunStatus.SUCCEEDED,
                startedAt,
                model.toRunSnapshot(),
                text,
                snapshot.speechOutputSettings.languageCode,
                voice.id,
                metrics,
                null,
            )
            activeLinkedRunIds += runId
            mutableState.update {
                it.copy(operation = AssistantOperation.Idle, speakingMessageId = null, statusMessage = null)
            }
            SpeechOutcome(succeeded = true)
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                activeLinkedRunIds += runRecorder.recordSpeechOutput(
                    RunStatus.CANCELLED,
                    startedAt,
                    model.toRunSnapshot(),
                    text,
                    snapshot.speechOutputSettings.languageCode,
                    voice.id,
                    metrics,
                    "Speech playback cancelled.",
                )
            }
            mutableState.update {
                it.copy(operation = AssistantOperation.Idle, speakingMessageId = null, statusMessage = "Speech stopped.")
            }
            throw cancelled
        } catch (error: Throwable) {
            val message = error.message ?: "Could not speak this response."
            withContext(NonCancellable) {
                activeLinkedRunIds += runRecorder.recordSpeechOutput(
                    RunStatus.FAILED,
                    startedAt,
                    model.toRunSnapshot(),
                    text,
                    snapshot.speechOutputSettings.languageCode,
                    voice.id,
                    metrics,
                    message,
                )
            }
            mutableState.update {
                it.copy(operation = AssistantOperation.Idle, speakingMessageId = null, errorMessage = message, statusMessage = null)
            }
            SpeechOutcome(succeeded = false, error = message)
        }
    }

    private fun launchForeground(block: suspend () -> Unit) {
        if (activeJob?.isActive == true) return
        activeLinkedRunIds.clear()
        val job = viewModelScope.launch(Dispatchers.Default) { block() }
        activeJob = job
        val registration = operationCoordinator.register(::cancel)
        job.invokeOnCompletion {
            registration.close()
            if (activeJob === job) activeJob = null
        }
    }

    private fun handleOperationFailure(error: Throwable) {
        if (error is CancellationException) return
        Log.e(TAG, "Assistant operation failed: ${error.message}", error)
        mutableState.update {
            it.copy(
                operation = AssistantOperation.Idle,
                level = null,
                speakingMessageId = null,
                errorMessage = error.message ?: "The local assistant operation failed.",
            )
        }
    }

    private fun persistPreferences() {
        val preferences = mutableState.value.toPreferences()
        viewModelScope.launch(Dispatchers.IO) { preferencesRepository.update(preferences) }
    }

    private fun applyReplay(run: RunRecord) {
        val parameters = runCatching { Json.parseToJsonElement(run.parametersJson).jsonObject }.getOrNull()
        val modelId = run.model?.modelId?.let(::ModelId)
        mutableState.update { state ->
            state.copy(
                selectedChatModelId = modelId?.takeIf { candidate -> state.chatModels.any { it.id == candidate && it.installed } }
                    ?: state.selectedChatModelId,
                inputMode = if (run.capability == AiCapability.VOICE_ASSISTANT && state.voiceConfigurationError == null) {
                    AssistantInputMode.VOICE
                } else {
                    AssistantInputMode.DICTATE
                },
                input = run.input.orEmpty(),
                chatSettings = state.chatSettings.copy(
                    computePreference = parameters?.get("computePreference")?.jsonPrimitive?.content
                        ?.let { stored -> ComputePreference.entries.firstOrNull { it.name == stored } }
                        ?: state.chatSettings.computePreference,
                    systemPrompt = parameters?.get("systemPrompt")?.jsonPrimitive?.content ?: state.chatSettings.systemPrompt,
                    temperature = parameters?.get("temperature")?.jsonPrimitive?.content ?: state.chatSettings.temperature,
                    topK = parameters?.get("topK")?.jsonPrimitive?.content ?: state.chatSettings.topK,
                    topP = parameters?.get("topP")?.jsonPrimitive?.content ?: state.chatSettings.topP,
                    maxOutputTokens = parameters?.get("maxOutputTokens")?.jsonPrimitive?.content ?: state.chatSettings.maxOutputTokens,
                    seed = parameters?.get("seed")?.jsonPrimitive?.contentOrNull
                        ?.takeUnless { it == "-1" }
                        ?: "",
                    contextSize = parameters?.get("contextSize")?.jsonPrimitive?.content ?: state.chatSettings.contextSize,
                    threadCount = parameters?.get("threadCount")?.jsonPrimitive?.content ?: state.chatSettings.threadCount,
                ),
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
            )
        },
        errorMessage = error,
        messages = messages.map { message ->
            AssistantConversationSnapshot(
                id = message.id,
                role = if (message.role == ChatMessageRole.USER) ConversationMessageRole.USER else ConversationMessageRole.ASSISTANT,
                content = message.content,
                incomplete = message.streaming ||
                    (incompleteAssistant && message.role == ChatMessageRole.ASSISTANT && message.failed),
            )
        },
    )

    override fun onCleared() {
        audioRecorder.stop()
        transcriber.cancel()
        chatEngine.cancel()
        speechOutput.cancel()
        activeJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val TAG = "AiP123Assistant"
    }
}

private data class AssistantModelOptions(
    val chat: List<ChatModelOption>,
    val speech: List<SpeechModelOption>,
    val voice: List<TtsModelOption>,
)

private data class GenerationOutcome(
    val text: String,
    val speechSucceeded: Boolean,
    val speechError: String?,
)

private data class SpeechOutcome(
    val succeeded: Boolean = true,
    val error: String? = null,
)

private fun AssistantUiState.withConfiguration(
    options: AssistantModelOptions,
    preferences: AssistantPreferences,
): AssistantUiState {
    val chatId = preferences.chat.modelId?.let(::ModelId)
        ?.takeIf { id -> options.chat.any { it.id == id && it.installed } }
        ?: selectedChatModelId?.takeIf { id -> options.chat.any { it.id == id && it.installed } }
        ?: options.chat.firstOrNull { it.installed }?.id
    val speechId = preferences.speechInput.modelId?.let(::ModelId)
        ?.takeIf { id -> options.speech.any { it.id == id && it.installed } }
        ?: selectedSpeechModelId?.takeIf { id -> options.speech.any { it.id == id && it.installed } }
        ?: options.speech.firstOrNull { it.installed }?.id
    val speechModel = options.speech.firstOrNull { it.id == speechId }
    val speechLanguage = preferences.speechInput.languageCode.takeIf { speechModel?.supports(it) == true }
        ?: speechModel?.languages?.firstOrNull()?.let(::normalizeLanguageCode)
        ?: "en"
    val voiceId = preferences.speechOutput.modelId?.let(::ModelId)
        ?.takeIf { id -> options.voice.any { it.id == id && it.installed } }
        ?: selectedVoiceModelId?.takeIf { id -> options.voice.any { it.id == id && it.installed } }
        ?: options.voice.firstOrNull { it.installed }?.id
    val voiceModel = options.voice.firstOrNull { it.id == voiceId }
    val outputLanguage = preferences.speechOutput.languageCode.takeIf { language ->
        voiceModel?.languages?.isEmpty() == true || voiceModel?.languages?.any { normalizeLanguageCode(it) == language } == true
    } ?: voiceModel?.languages?.firstOrNull()?.let(::normalizeLanguageCode) ?: "en"
    val compatibleVoices = voiceModel?.compatibleVoices(outputLanguage).orEmpty()
    val selectedOutputVoice = preferences.speechOutput.voiceId?.takeIf { id -> compatibleVoices.any { it.id == id } }
        ?: selectedVoiceId?.takeIf { id -> compatibleVoices.any { it.id == id } }
        ?: compatibleVoices.firstOrNull()?.id
    val chatModel = options.chat.firstOrNull { it.id == chatId }
    val restoredChatSettings = preferences.chat.toUi()
    return copy(
        chatModels = options.chat,
        speechModels = options.speech,
        voiceModels = options.voice,
        selectedChatModelId = chatId,
        selectedSpeechModelId = speechId,
        selectedVoiceModelId = voiceId,
        selectedVoiceId = selectedOutputVoice,
        chatSettings = restoredChatSettings.copy(
            computePreference = chatModel?.supportedComputePreference(restoredChatSettings.computePreference)
                ?: restoredChatSettings.computePreference,
        ),
        speechInputSettings = SpeechInputSettings(speechLanguage, preferences.speechInput.threadCount.toString()),
        speechOutputSettings = SpeechOutputSettings(
            languageCode = outputLanguage,
            speed = preferences.speechOutput.speed.toString(),
            volume = preferences.speechOutput.volume.toString(),
            sentenceSilenceScale = preferences.speechOutput.sentenceSilenceScale.toString(),
            threadCount = preferences.speechOutput.threadCount.toString(),
        ),
    )
}

private fun AssistantChatPreferences.toUi() = ChatSettings(
    computePreference = computePreference,
    systemPrompt = systemPrompt,
    temperature = temperature.toString(),
    topK = topK.toString(),
    topP = topP.toString(),
    maxOutputTokens = maxOutputTokens.toString(),
    seed = seed?.toString().orEmpty(),
    contextSize = contextSize.toString(),
    threadCount = threadCount.toString(),
)

private fun AssistantUiState.toPreferences() = AssistantPreferences(
    chat = chatSettings.toEffectiveOrDefault(selectedChatModel?.defaultContextSize ?: 512).let { settings ->
        AssistantChatPreferences(
            modelId = selectedChatModelId?.value,
            computePreference = settings.computePreference,
            systemPrompt = settings.systemPrompt,
            temperature = settings.temperature,
            topK = settings.topK,
            topP = settings.topP,
            maxOutputTokens = settings.maxOutputTokens,
            seed = settings.seed,
            contextSize = settings.contextSize,
            threadCount = settings.threadCount,
        )
    },
    speechInput = AssistantSpeechInputPreferences(
        modelId = selectedSpeechModelId?.value,
        languageCode = speechInputSettings.languageCode,
        threadCount = speechInputSettings.threadCount.toIntOrNull()?.coerceIn(0, 64) ?: 0,
    ),
    speechOutput = AssistantSpeechOutputPreferences(
        modelId = selectedVoiceModelId?.value,
        voiceId = selectedVoiceId,
        languageCode = speechOutputSettings.languageCode,
        speed = speechOutputSettings.speed.toFloatOrNull()?.coerceIn(0.5f, 2f) ?: 1f,
        volume = speechOutputSettings.volume.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f,
        sentenceSilenceScale = speechOutputSettings.sentenceSilenceScale.toFloatOrNull()?.coerceIn(0f, 2f) ?: 1f,
        threadCount = speechOutputSettings.threadCount.toIntOrNull()?.coerceIn(0, 64) ?: 0,
    ),
)

private fun ChatSettings.toEffectiveOrDefault(contextSize: Int): EffectiveChatSettings =
    runCatching(::toEffective).getOrElse { ChatSettings(contextSize = contextSize.toString()).toEffective() }

private fun SpeechModelOption.toRunSnapshot() = RunModelSnapshot(id.value, displayName, engineId.value)

private fun TtsModelOption.toRunSnapshot() = RunModelSnapshot(id.value, displayName, engineId.value)

private fun appendTranscript(existing: String, transcript: String): String = when {
    existing.isBlank() -> transcript
    existing.last().isWhitespace() -> existing + transcript
    else -> "$existing $transcript"
}

private fun rate(tokens: Int?, durationMs: Long): Double? =
    tokens?.let { count -> durationMs.takeIf { it > 0 }?.let { count * 1_000.0 / it } }
