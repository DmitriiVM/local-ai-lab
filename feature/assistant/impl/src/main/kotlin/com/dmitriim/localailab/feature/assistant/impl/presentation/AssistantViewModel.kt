package com.dmitriim.localailab.feature.assistant.impl.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.chat.ChatEngine
import com.dmitriim.localailab.ai.api.chat.LlmChatMessage
import com.dmitriim.localailab.ai.api.chat.LlmChatRole
import com.dmitriim.localailab.ai.api.memory.AiRuntimeKind
import com.dmitriim.localailab.ai.api.memory.AiRuntimeLeaseManager
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.system.SystemSpeechToTextSupport
import com.dmitriim.localailab.ai.api.system.SystemTextToSpeechSupport
import com.dmitriim.localailab.ai.runtime.memory.FeatureRuntimeLeaseController
import com.dmitriim.localailab.core.audio.input.storage.ReferenceVoiceStore
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.operation.ForegroundOperationCoordinator
import com.dmitriim.localailab.feature.assistant.impl.domain.AssistantRunRecorder
import com.dmitriim.localailab.feature.assistant.impl.domain.chat.GenerateAssistantResponse
import com.dmitriim.localailab.feature.assistant.impl.domain.chat.PersistAssistantTurn
import com.dmitriim.localailab.feature.assistant.impl.domain.stt.AssistantAudioRecorder
import com.dmitriim.localailab.feature.assistant.impl.domain.stt.AssistantTranscriber
import com.dmitriim.localailab.feature.assistant.impl.domain.tts.AssistantSpeechOutput
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.AssistantInputMode
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.AssistantUiState
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.ChatMessageRole
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.ChatSettings
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.SpeechInputSettings
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.SpeechOutputSettings
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkWorkload
import com.dmitriim.localailab.feature.benchmark.api.launch.ProfileWorkloadStore
import com.dmitriim.localailab.feature.models.api.data.ModelLibrary
import com.dmitriim.localailab.feature.models.api.data.ModelTransfers
import com.dmitriim.localailab.feature.runs.api.data.RunRepository
import com.dmitriim.localailab.feature.runs.api.domain.history.RunRecord
import com.dmitriim.localailab.feature.runs.api.domain.replay.RunReplay
import com.dmitriim.localailab.feature.settings.api.data.AssistantPreferencesRepository
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    generateResponse: GenerateAssistantResponse,
    audioRecorder: AssistantAudioRecorder,
    transcriber: AssistantTranscriber,
    speechOutput: AssistantSpeechOutput,
    runRecorder: AssistantRunRecorder,
    persistAssistantTurn: PersistAssistantTurn,
    private val preferencesRepository: AssistantPreferencesRepository,
    operationCoordinator: ForegroundOperationCoordinator,
    private val runRepository: RunRepository,
    private val replayStore: RunReplay,
    private val profileWorkloadStore: ProfileWorkloadStore,
    runtimeLeaseManager: AiRuntimeLeaseManager,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AssistantUiState())
    val state: StateFlow<AssistantUiState> = mutableState.asStateFlow()
    private var conversationId = UUID.randomUUID().toString()

    private val operationController = AssistantOperationController(
        scope = viewModelScope,
        state = mutableState,
        chatEngine = chatEngine,
        generateResponse = generateResponse,
        audioRecorder = audioRecorder,
        transcriber = transcriber,
        speechOutput = speechOutput,
        runRecorder = runRecorder,
        persistAssistantTurn = persistAssistantTurn,
        operationCoordinator = operationCoordinator,
        conversationId = { conversationId },
    )
    internal val runtimeLeaseController = FeatureRuntimeLeaseController(
        leaseManager = runtimeLeaseManager,
        runtimeKinds = setOf(
            AiRuntimeKind.CHAT,
            AiRuntimeKind.SPEECH_TO_TEXT,
            AiRuntimeKind.TEXT_TO_SPEECH,
        ),
        onRelease = operationController::clear,
    )

    init {
        refreshSystemVoices()
        observeConfiguration()
        observeReplayRequests()
    }

    fun updateInput(value: String) = mutableState.update {
        it.copy(input = value, errorMessage = null, statusMessage = null)
    }

    fun selectInputMode(mode: AssistantInputMode) {
        if (!mutableState.value.isIdle) return
        if (mode == AssistantInputMode.VOICE) {
            if (mutableState.value.voiceConfigurationError != null) return
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
        if (modelChanged) operationController.unloadChatRuntime()
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

    fun applySpeechOutputSettings(modelId: ModelId, voiceId: String, settings: SpeechOutputSettings): String? {
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
        if (modelChanged) operationController.unloadSpeechOutputRuntime()
        persistPreferences()
        return null
    }

    fun unloadChatModel() = operationController.unloadChatRuntime()

    fun send() = operationController.send()

    fun prepareProfile(): Boolean {
        val snapshot = mutableState.value
        if (!snapshot.canSend) return false
        val model = snapshot.selectedChatModel ?: return false
        val settings = runCatching(snapshot.chatSettings::toEffective).getOrElse { error ->
            mutableState.update { it.copy(errorMessage = error.message ?: "Chat settings are invalid.") }
            return false
        }
        val messages = buildList {
            if (settings.systemPrompt.isNotBlank()) {
                add(LlmChatMessage(LlmChatRole.SYSTEM, settings.systemPrompt))
            }
            snapshot.messages
                .filterNot { it.streaming || it.failed }
                .mapTo(this) { message ->
                    LlmChatMessage(
                        role = when (message.role) {
                            ChatMessageRole.USER -> LlmChatRole.USER
                            ChatMessageRole.ASSISTANT -> LlmChatRole.ASSISTANT
                        },
                        content = message.content,
                    )
                }
            add(LlmChatMessage(LlmChatRole.USER, snapshot.input.trim()))
        }
        profileWorkloadStore.open(
            BenchmarkWorkload.Chat(
                modelId = model.id,
                modelDisplayName = model.displayName,
                computePreference = settings.computePreference,
                messages = messages,
                maxTokens = settings.maxOutputTokens,
                temperature = settings.temperature,
                topK = settings.topK,
                topP = settings.topP,
                seed = settings.seed,
                contextSize = settings.contextSize,
                threadCount = settings.threadCount,
            ),
        )
        return true
    }

    fun startRecording() = operationController.startRecording()

    fun stopRecording() = operationController.stopRecording()

    fun microphonePermissionDenied() = mutableState.update {
        it.copy(errorMessage = "Microphone permission is required for voice input.")
    }

    fun speakMessage(messageId: String) = operationController.speakMessage(messageId)

    fun previewVoice(modelId: ModelId, voiceId: String, settings: SpeechOutputSettings): String? = operationController
        .previewVoice(modelId, voiceId, settings)

    fun regenerate() = operationController.regenerate()

    fun editAndRetry(messageId: String) {
        val snapshot = mutableState.value
        if (!snapshot.isIdle) return
        val index = snapshot.messages.indexOfFirst { it.id == messageId && it.role == ChatMessageRole.USER }
        if (index < 0) return
        mutableState.update {
            it.copy(
                messages = snapshot.messages.take(index),
                input = snapshot.messages[index].content,
                metrics = null,
                errorMessage = null,
            )
        }
    }

    fun clearConversation() {
        if (!mutableState.value.isIdle) return
        val oldId = conversationId
        conversationId = UUID.randomUUID().toString()
        mutableState.update {
            it.copy(
                messages = emptyList(),
                input = "",
                metrics = null,
                contextUsage = null,
                statusMessage = null,
                errorMessage = null,
            )
        }
        viewModelScope.launch(Dispatchers.IO) { runRepository.deleteConversation(oldId) }
    }

    fun cancel() = operationController.cancel()

    override fun onCleared() {
        runtimeLeaseController.onHidden()
        super.onCleared()
    }

    private fun refreshSystemVoices() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching(systemTextToSpeechSupport::refresh)
                .onFailure { Log.w(TAG, "Android TTS discovery failed: ${it.message}") }
        }
    }

    private fun observeConfiguration() {
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
                if (effectivePreferences != preferences) preferencesRepository.update(effectivePreferences)
            }
        }
    }

    private fun observeReplayRequests() {
        viewModelScope.launch {
            replayStore.pending.collectLatest { run ->
                run
                    ?.takeIf { it.capability in setOf(AiCapability.CHAT, AiCapability.VOICE_ASSISTANT) }
                    ?.let(::applyReplay)
            }
        }
    }

    private fun applyReplay(run: RunRecord) {
        mutableState.update { AssistantReplayRestorer.restore(it, run) }
        replayStore.consume(run.id)
    }

    private fun persistPreferences() {
        val preferences = mutableState.value.toPreferences()
        viewModelScope.launch(Dispatchers.IO) { preferencesRepository.update(preferences) }
    }

    private companion object {
        const val TAG = "AiP123Assistant"
    }
}
