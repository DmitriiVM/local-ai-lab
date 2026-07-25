package com.dmitriim.localaiplayground.feature.voice.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelRepository
import com.dmitriim.localaiplayground.core.result.ForegroundOperationCoordinator
import com.dmitriim.localaiplayground.feature.voice.domain.VoiceAssistantCoordinator
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

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class VoiceViewModel(
    private val modelRepository: ModelRepository,
    private val coordinator: VoiceAssistantCoordinator,
    private val operationCoordinator: ForegroundOperationCoordinator,
) : ViewModel() {
    private val mutableState = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = mutableState.asStateFlow()
    private var turnJob: Job? = null

    init {
        viewModelScope.launch {
            modelRepository.installedModels.collectLatest { installed ->
                mutableState.update { it.withAvailableModels(installed) }
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
        if (turnJob?.isActive == true) return
        val snapshot = mutableState.value
        val configurationError = snapshot.configurationError
        if (configurationError != null) {
            mutableState.update { it.copy(errorMessage = configurationError, phase = VoicePhase.ERROR) }
            return
        }
        val request = snapshot.toVoiceTurnRequest().getOrElse { error ->
            mutableState.update {
                it.copy(
                    phase = VoicePhase.ERROR,
                    errorMessage = error.message ?: "Voice settings are invalid.",
                )
            }
            return
        }
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
                coordinator.execute(request).collect { event ->
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
            } catch (cancelled: CancellationException) {
                mutableState.update {
                    it.copy(phase = VoicePhase.IDLE, level = null, statusMessage = "Voice turn cancelled.")
                }
            } catch (error: Throwable) {
                mutableState.update {
                    it.copy(
                        phase = VoicePhase.ERROR,
                        level = null,
                        errorMessage = error.message ?: "The local voice pipeline failed.",
                        statusMessage = null,
                    )
                }
            }
        }.also(::registerForegroundCancellation)
    }

    fun stopListening() {
        if (mutableState.value.phase != VoicePhase.LISTENING) return
        mutableState.update { it.copy(phase = VoicePhase.FINALIZING, level = null, statusMessage = "Finalizing recorded speech…") }
        coordinator.stopListening()
    }

    fun cancel() {
        if (turnJob?.isActive != true) return
        mutableState.update { it.copy(phase = VoicePhase.CANCELLING, statusMessage = "Stopping local engines and audio…") }
        coordinator.cancel()
        turnJob?.cancel()
    }

    fun newConversation() {
        if (turnJob?.isActive == true) return
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
    }

    fun microphonePermissionDenied() = mutableState.update {
        it.copy(phase = VoicePhase.ERROR, errorMessage = "Microphone permission is required for push-to-talk.")
    }

    private fun selectWhenInactive(transform: (VoiceUiState) -> VoiceUiState) {
        if (turnJob?.isActive == true) return
        mutableState.update { transform(it).copy(errorMessage = null, statusMessage = null) }
    }

    private fun registerForegroundCancellation(job: Job) {
        val registration = operationCoordinator.register(::cancel)
        job.invokeOnCompletion { registration.close() }
    }

    override fun onCleared() {
        coordinator.cancel()
        turnJob?.cancel()
        super.onCleared()
    }
}
