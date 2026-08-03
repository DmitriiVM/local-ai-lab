package com.dmitriim.localaiplayground.feature.assistant.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localaiplayground.ai.api.llm.ChatEngine
import com.dmitriim.localaiplayground.ai.api.system.SystemSpeechToTextSupport
import com.dmitriim.localaiplayground.ai.api.system.SystemTextToSpeechSupport
import com.dmitriim.localaiplayground.core.audio.input.storage.ReferenceVoiceStore
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.engine.ComputePreference
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.model.runs.RunRecord
import com.dmitriim.localaiplayground.core.model.service.ModelLibrary
import com.dmitriim.localaiplayground.core.model.service.ModelTransfers
import com.dmitriim.localaiplayground.core.model.service.RunRepository
import com.dmitriim.localaiplayground.core.result.ForegroundOperationCoordinator
import com.dmitriim.localaiplayground.feature.assistant.domain.AssistantAudioRecorder
import com.dmitriim.localaiplayground.feature.assistant.domain.AssistantRunRecorder
import com.dmitriim.localaiplayground.feature.assistant.domain.AssistantSpeechOutput
import com.dmitriim.localaiplayground.feature.assistant.domain.AssistantTranscriber
import com.dmitriim.localaiplayground.feature.assistant.domain.GenerateAssistantResponse
import com.dmitriim.localaiplayground.feature.assistant.domain.PersistAssistantTurn
import com.dmitriim.localaiplayground.source.runs.RunReplayStore
import com.dmitriim.localaiplayground.source.settings.AssistantPreferencesRepository
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
    private val replayStore: RunReplayStore,
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

    fun startRecording() = operationController.startRecording()

    fun stopRecording() = operationController.stopRecording()

    fun microphonePermissionDenied() = mutableState.update {
        it.copy(errorMessage = "Microphone permission is required for voice input.")
    }

    fun speakMessage(messageId: String) = operationController.speakMessage(messageId)

    fun previewVoice(modelId: ModelId, voiceId: String, settings: SpeechOutputSettings): String? =
        operationController.previewVoice(modelId, voiceId, settings)

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
            it.copy(messages = emptyList(), input = "", metrics = null, contextUsage = null, statusMessage = null, errorMessage = null)
        }
        viewModelScope.launch(Dispatchers.IO) { runRepository.deleteConversation(oldId) }
    }

    fun cancel() = operationController.cancel()

    override fun onCleared() {
        operationController.clear()
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
