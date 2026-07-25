package com.dmitriim.localaiplayground.feature.stt.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localaiplayground.core.audio.input.model.PcmAudioInput
import com.dmitriim.localaiplayground.core.audio.input.storage.AudioInputStore
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelRepository
import com.dmitriim.localaiplayground.core.result.ForegroundOperationCoordinator
import com.dmitriim.localaiplayground.feature.stt.domain.SpeechTranscriptionEvent
import com.dmitriim.localaiplayground.feature.stt.domain.SpeechTranscriptionRequest
import com.dmitriim.localaiplayground.feature.stt.domain.SttTranscriptionSettings
import com.dmitriim.localaiplayground.feature.stt.domain.TranscribeAudio
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
class SpeechToTextViewModel(
    private val modelRepository: ModelRepository,
    private val audioInputStore: AudioInputStore,
    private val transcribeAudio: TranscribeAudio,
    private val operationCoordinator: ForegroundOperationCoordinator,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SpeechToTextUiState())
    val state: StateFlow<SpeechToTextUiState> = mutableState.asStateFlow()
    private var operationJob: Job? = null

    init {
        viewModelScope.launch {
            modelRepository.installedModels.collectLatest { installed ->
                val models = installed.filter { it.isReadySpeechModel() }.map { it.toSpeechModelOption() }
                mutableState.update { current ->
                    val selected = current.selectedModelId?.takeIf { id -> models.any { it.id == id } } ?: models.firstOrNull()?.id
                    current.copy(models = models, selectedModelId = selected)
                }
            }
        }
    }

    fun selectModel(modelId: ModelId) {
        if (operationJob?.isActive == true) return
        mutableState.update { it.copy(selectedModelId = modelId, errorMessage = null, metrics = null) }
    }

    fun selectLanguage(language: SttLanguage) = mutableState.update { it.copy(language = language, errorMessage = null) }

    fun updateThreadCount(value: String) = mutableState.update { it.copy(threadCount = value.filter(Char::isDigit), errorMessage = null) }

    fun startRecording() {
        if (operationJob?.isActive == true) return
        if (!requireModel()) return
        mutableState.update { it.copy(operation = SttOperation.RECORDING, transcript = "", metrics = null, errorMessage = null, level = null) }
        operationJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val input = audioInputStore.capture { level ->
                    mutableState.update { it.copy(level = level) }
                }
                replaceInput(input)
                mutableState.update { it.copy(level = null, operation = SttOperation.IDLE) }
                transcribe(input)
            } catch (cancelled: CancellationException) {
                mutableState.update { it.copy(operation = SttOperation.IDLE, level = null) }
            } catch (error: Throwable) {
                mutableState.update { it.copy(operation = SttOperation.IDLE, level = null, errorMessage = error.message ?: "Microphone capture failed.") }
            }
        }.also(::registerForegroundCancellation)
    }

    fun stopRecording() {
        if (mutableState.value.operation != SttOperation.RECORDING) return
        mutableState.update { it.copy(operation = SttOperation.STOPPING) }
        audioInputStore.stopCapture()
    }

    fun importAudio(uri: Uri) {
        if (operationJob?.isActive == true) return
        if (!requireModel()) return
        mutableState.update { it.copy(operation = SttOperation.IMPORTING, transcript = "", metrics = null, errorMessage = null) }
        operationJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val input = audioInputStore.importAudio(uri)
                replaceInput(input)
                mutableState.update { it.copy(operation = SttOperation.IDLE) }
                transcribe(input)
            } catch (cancelled: CancellationException) {
                mutableState.update { it.copy(operation = SttOperation.IDLE) }
            } catch (error: Throwable) {
                mutableState.update { it.copy(operation = SttOperation.IDLE, errorMessage = error.message ?: "Audio import failed.") }
            }
        }.also(::registerForegroundCancellation)
    }

    fun repeatTranscription() {
        if (operationJob?.isActive == true) return
        mutableState.value.input?.let(::transcribe)
    }

    fun cancel() {
        if (operationJob?.isActive != true) return
        mutableState.update { it.copy(operation = SttOperation.CANCELLING) }
        audioInputStore.stopCapture()
        transcribeAudio.cancel()
        operationJob?.cancel()
    }

    fun clear() {
        if (operationJob?.isActive == true) cancel()
        val input = mutableState.value.input
        audioInputStore.clear(input)
        mutableState.update { it.copy(input = null, transcript = "", metrics = null, level = null, errorMessage = null, operation = SttOperation.IDLE) }
    }

    fun microphonePermissionDenied() = mutableState.update {
        it.copy(errorMessage = "Microphone permission was denied. You can allow it in Android settings and try again.")
    }

    private fun transcribe(input: PcmAudioInput) {
        val snapshot = mutableState.value
        val modelId = snapshot.selectedModelId ?: return
        mutableState.update { it.copy(operation = SttOperation.TRANSCRIBING, transcript = "", metrics = null, errorMessage = null) }
        operationJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                transcribeAudio.execute(
                    SpeechTranscriptionRequest(
                        modelId = modelId,
                        input = input,
                        settings = SttTranscriptionSettings(
                            languageCode = snapshot.language.whisperCode,
                            threadCount = snapshot.threadCount,
                        ),
                    ),
                ).collect { event ->
                    when (event) {
                        is SpeechTranscriptionEvent.Prepared -> Unit
                        is SpeechTranscriptionEvent.Completed -> mutableState.update {
                            it.copy(
                                operation = SttOperation.IDLE,
                                transcript = event.transcript,
                                metrics = event.metrics,
                            )
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                mutableState.update { it.copy(operation = SttOperation.IDLE, errorMessage = "Transcription cancelled.") }
            } catch (error: Throwable) {
                mutableState.update { it.copy(operation = SttOperation.IDLE, errorMessage = error.message ?: "Local transcription failed.") }
            }
        }.also(::registerForegroundCancellation)
    }

    private fun requireModel(): Boolean {
        if (mutableState.value.selectedModelId != null) return true
        mutableState.update { it.copy(errorMessage = "Install a compatible Whisper speech model before recording or importing audio.") }
        return false
    }

    private fun replaceInput(input: PcmAudioInput) {
        val previous = mutableState.value.input
        if (previous != null && previous.file != input.file) audioInputStore.clear(previous)
        mutableState.update { it.copy(input = input) }
    }

    private fun registerForegroundCancellation(job: Job) {
        val registration = operationCoordinator.register(::cancel)
        job.invokeOnCompletion { registration.close() }
    }

    override fun onCleared() {
        cancel()
        super.onCleared()
    }
}
