package com.dmitriim.localailab.feature.stt.impl.presentation

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.memory.AiRuntimeKind
import com.dmitriim.localailab.ai.api.memory.AiRuntimeLeaseManager
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.system.SystemSpeechToTextSupport
import com.dmitriim.localailab.ai.runtime.memory.FeatureRuntimeLeaseController
import com.dmitriim.localailab.core.audio.input.model.PcmAudioInput
import com.dmitriim.localailab.core.audio.input.storage.AudioInputStore
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.operation.ForegroundOperationCoordinator
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.text.UiText
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkWorkload
import com.dmitriim.localailab.feature.benchmark.api.launch.ProfileWorkloadStore
import com.dmitriim.localailab.feature.models.api.data.ModelLibrary
import com.dmitriim.localailab.feature.models.api.data.ModelTransfers
import com.dmitriim.localailab.feature.runs.api.domain.history.RunModelSnapshot
import com.dmitriim.localailab.feature.runs.api.domain.history.RunRecord
import com.dmitriim.localailab.feature.runs.api.domain.history.RunStatus
import com.dmitriim.localailab.feature.runs.api.domain.replay.RunReplay
import com.dmitriim.localailab.feature.settings.api.data.AppSettingsRepository
import com.dmitriim.localailab.feature.stt.api.domain.SpeechTranscriptionEvent
import com.dmitriim.localailab.feature.stt.api.domain.SpeechTranscriptionMetrics
import com.dmitriim.localailab.feature.stt.api.domain.SpeechTranscriptionRequest
import com.dmitriim.localailab.feature.stt.api.domain.SttTranscriptionSettings
import com.dmitriim.localailab.feature.stt.api.domain.TranscribeAudio
import com.dmitriim.localailab.feature.stt.impl.domain.PersistSttRun
import com.dmitriim.localailab.feature.stt.impl.domain.SttRunSnapshot
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class SpeechToTextViewModel(
    private val modelLibrary: ModelLibrary,
    private val modelTransfers: ModelTransfers,
    private val audioInputStore: AudioInputStore,
    private val transcribeAudio: TranscribeAudio,
    private val operationCoordinator: ForegroundOperationCoordinator,
    private val persistSttRun: PersistSttRun,
    private val replayStore: RunReplay,
    private val settingsRepository: AppSettingsRepository,
    private val systemSpeechSupport: SystemSpeechToTextSupport,
    private val profileWorkloadStore: ProfileWorkloadStore,
    runtimeLeaseManager: AiRuntimeLeaseManager,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SpeechToTextUiState())
    val state: StateFlow<SpeechToTextUiState> = mutableState.asStateFlow()
    private var operationJob: Job? = null
    private var savedModelId: ModelId? = null
    internal val runtimeLeaseController = FeatureRuntimeLeaseController(
        leaseManager = runtimeLeaseManager,
        runtimeKinds = setOf(AiRuntimeKind.SPEECH_TO_TEXT),
        onRelease = ::cancel,
    )

    init {
        viewModelScope.launch {
            savedModelId = settingsRepository.sttSelectedModel.first()?.let(::ModelId)
            combine(modelLibrary.installedModels, modelTransfers.catalog, ::Pair).collectLatest { (installed, catalog) ->
                val installedById = installed.filter { it.isReadySpeechModel() }.associateBy { it.manifest.modelId }
                val catalogSpeechModels = catalog.filter {
                    AiCapability.SPEECH_TO_TEXT in it.manifest.capabilities
                }
                val models = buildList {
                    if (systemSpeechSupport.isOnDeviceRecognizerAvailable) {
                        add(androidSpeechRecognizerOption())
                    }
                    catalogSpeechModels.forEach { entry ->
                        add(installedById[entry.manifest.modelId]?.toSpeechModelOption() ?: entry.toSpeechModelOption())
                    }
                    installedById
                        .filterKeys { id -> catalogSpeechModels.none { it.manifest.modelId == id } }
                        .values
                        .mapTo(this) { it.toSpeechModelOption() }
                }
                mutableState.update { current ->
                    val selected = current.selectedModelId
                        ?.takeIf { id -> models.any { it.id == id && it.installed } }
                        ?: savedModelId?.takeIf { id -> models.any { it.id == id && it.installed } }
                        ?: models.firstOrNull { it.installed }?.id
                    val selectedModel = models.firstOrNull { it.id == selected }
                    val language = current.language.takeIf { it in selectedModel?.supportedLanguages.orEmpty() }
                        ?: selectedModel?.supportedLanguages?.firstOrNull()
                        ?: current.language
                    current.copy(models = models, selectedModelId = selected, language = language)
                }
                mutableState.value.selectedModelId?.let { selectedModelId ->
                    if (savedModelId != selectedModelId) {
                        savedModelId = selectedModelId
                        persistModelSelection(selectedModelId)
                    }
                }
            }
        }
        viewModelScope.launch {
            replayStore.pending.collectLatest { run ->
                if (run?.capability == AiCapability.SPEECH_TO_TEXT) applyReplay(run)
            }
        }
    }

    fun selectModel(modelId: ModelId) {
        if (operationJob?.isActive == true) return
        if (mutableState.value.models.none { it.id == modelId && it.installed }) return
        mutableState.update { current ->
            val model = current.models.first { it.id == modelId }
            current.copy(
                selectedModelId = modelId,
                language = current.language.takeIf { it in model.supportedLanguages }
                    ?: model.supportedLanguages.firstOrNull()
                    ?: current.language,
                errorMessage = null,
                metrics = null,
            )
        }
        savedModelId = modelId
        persistModelSelection(modelId)
    }

    fun selectLanguage(language: SttLanguage) = mutableState.update {
        if (language in it.availableLanguages) it.copy(language = language, errorMessage = null) else it
    }

    fun updateThreadCount(value: String) = mutableState.update { it.copy(threadCount = value.filter(Char::isDigit), errorMessage = null) }

    fun startRecording() {
        if (operationJob?.isActive == true) {
            Log.w(TAG, "Ignoring recording request because an STT operation is already active.")
            return
        }
        if (!requireModel()) return
        Log.i(TAG, "STT UI recording started: language=${mutableState.value.language.whisperCode}, threads=${mutableState.value.threadCount}")
        mutableState.update { it.copy(operation = SttOperation.RECORDING, transcript = "", metrics = null, errorMessage = null, level = null) }
        operationJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val input = audioInputStore.capture { level ->
                    mutableState.update { it.copy(level = level) }
                }
                Log.i(TAG, "STT UI recording captured: durationMs=${input.durationMs}, sampleRateHz=${input.sampleRateHz}")
                replaceInput(input)
                mutableState.update { it.copy(level = null, operation = SttOperation.IDLE) }
                transcribe(input)
            } catch (_: CancellationException) {
                Log.i(TAG, "STT UI recording cancelled.")
                mutableState.update { it.copy(operation = SttOperation.IDLE, level = null) }
            } catch (error: Throwable) {
                Log.e(TAG, "STT UI recording failed: ${error.message}", error)
                mutableState.update { it.copy(operation = SttOperation.IDLE, level = null, errorMessage = error.message?.let(UiText::Dynamic) ?: UiText.Resource(CoreUiR.string.stt_error_microphone_capture)) }
            }
        }.also(::registerForegroundCancellation)
    }

    fun stopRecording() {
        if (mutableState.value.operation != SttOperation.RECORDING) return
        Log.i(TAG, "STT UI recording stop requested.")
        mutableState.update { it.copy(operation = SttOperation.STOPPING) }
        audioInputStore.stopCapture()
    }

    fun importAudio(uri: Uri) {
        if (operationJob?.isActive == true) {
            Log.w(TAG, "Ignoring import request because an STT operation is already active.")
            return
        }
        if (!requireModel()) return
        Log.i(TAG, "STT UI import started: uriScheme=${uri.scheme}")
        mutableState.update { it.copy(operation = SttOperation.IMPORTING, transcript = "", metrics = null, errorMessage = null) }
        operationJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val input = audioInputStore.importAudio(uri)
                Log.i(TAG, "STT UI import completed: durationMs=${input.durationMs}, sampleRateHz=${input.sampleRateHz}")
                replaceInput(input)
                mutableState.update { it.copy(operation = SttOperation.IDLE) }
                transcribe(input)
            } catch (_: CancellationException) {
                Log.i(TAG, "STT UI import cancelled.")
                mutableState.update { it.copy(operation = SttOperation.IDLE) }
            } catch (error: Throwable) {
                Log.e(TAG, "STT UI import failed: ${error.message}", error)
                mutableState.update { it.copy(operation = SttOperation.IDLE, errorMessage = error.message?.let(UiText::Dynamic) ?: UiText.Resource(CoreUiR.string.stt_error_audio_import)) }
            }
        }.also(::registerForegroundCancellation)
    }

    fun repeatTranscription() {
        if (operationJob?.isActive == true) {
            Log.w(TAG, "Ignoring repeat-transcription request because an STT operation is already active.")
            return
        }
        Log.i(TAG, "STT UI repeat transcription requested.")
        mutableState.value.input?.let(::transcribe)
    }

    fun prepareProfile(): Boolean {
        val snapshot = mutableState.value
        if (operationJob?.isActive == true || snapshot.operation != SttOperation.IDLE) return false
        val model = snapshot.selectedModel
        val input = snapshot.input
        val threads = snapshot.threadCount.toIntOrNull()
        val error = when {
            model?.installed != true -> UiText.Resource(CoreUiR.string.stt_error_select_model)
            input == null -> UiText.Resource(CoreUiR.string.stt_error_audio_before_profile)
            threads !in 0..64 -> UiText.Resource(CoreUiR.string.stt_error_thread_count_range)
            else -> null
        }
        if (error != null) {
            mutableState.update { it.copy(errorMessage = error) }
            return false
        }
        profileWorkloadStore.open(
            BenchmarkWorkload.SpeechToText(
                modelId = requireNotNull(model).id,
                modelDisplayName = model.displayName,
                input = requireNotNull(input),
                languageCode = snapshot.language.whisperCode,
                threadCount = snapshot.threadCount,
            ),
        )
        return true
    }

    fun cancel() {
        if (operationJob?.isActive != true) return
        Log.i(TAG, "STT UI cancellation requested: operation=${mutableState.value.operation}")
        mutableState.update { it.copy(operation = SttOperation.CANCELLING) }
        audioInputStore.stopCapture()
        transcribeAudio.cancel()
        operationJob?.cancel()
    }

    fun clear() {
        Log.i(TAG, "STT UI clear requested.")
        if (operationJob?.isActive == true) cancel()
        val input = mutableState.value.input
        audioInputStore.clear(input)
        mutableState.update { it.copy(input = null, transcript = "", metrics = null, level = null, errorMessage = null, operation = SttOperation.IDLE) }
    }

    fun microphonePermissionDenied() = mutableState.update {
        Log.w(TAG, "STT microphone permission denied.")
        it.copy(errorMessage = UiText.Resource(CoreUiR.string.stt_error_microphone_permission_denied))
    }

    private fun transcribe(input: PcmAudioInput) {
        val snapshot = mutableState.value
        val modelId = snapshot.selectedModelId ?: return
        val startedAt = System.currentTimeMillis()
        val runId = UUID.randomUUID().toString()
        val model = snapshot.selectedModel
        Log.i(
            TAG,
            "STT UI transcription started: model=${model?.displayName}, durationMs=${input.durationMs}, " +
                "sampleRateHz=${input.sampleRateHz}, language=${snapshot.language.whisperCode}, threads=${snapshot.threadCount}",
        )
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
                        runId = runId,
                    ),
                ).collect { event ->
                    when (event) {
                        is SpeechTranscriptionEvent.Prepared -> Log.i(
                            TAG,
                            "STT UI received prepared event: model=${event.modelName}, loadMs=${event.loadDurationMs}, threads=${event.effectiveThreadCount}",
                        )
                        is SpeechTranscriptionEvent.Completed -> mutableState.update {
                            it.copy(
                                operation = SttOperation.IDLE,
                                transcript = event.transcript,
                                metrics = event.metrics,
                                errorMessage = event.transcript.takeIf(String::isBlank)?.let {
                                    UiText.Resource(CoreUiR.string.stt_error_no_speech_recognized)
                                },
                            )
                        }.also {
                            Log.i(TAG, "STT UI received completed event: transcriptLength=${event.transcript.length}, segments=${event.metrics.segmentCount}, totalMs=${event.metrics.timeToFinalMs}")
                            persistSttRun(snapshotForPersistence(runId, RunStatus.SUCCEEDED, startedAt, model, input, event.transcript, snapshot, event.metrics, null))
                        }
                    }
                }
            } catch (_: CancellationException) {
                Log.i(TAG, "STT UI transcription cancelled.")
                mutableState.update { it.copy(operation = SttOperation.IDLE, errorMessage = UiText.Resource(CoreUiR.string.stt_error_transcription_cancelled)) }
                persistSttRun(snapshotForPersistence(runId, RunStatus.CANCELLED, startedAt, model, input, null, snapshot, null, "Transcription cancelled."))
            } catch (error: Throwable) {
                Log.e(TAG, "STT UI transcription failed: ${error.message}", error)
                mutableState.update { it.copy(operation = SttOperation.IDLE, errorMessage = error.message?.let(UiText::Dynamic) ?: UiText.Resource(CoreUiR.string.stt_error_transcription_failed)) }
                persistSttRun(snapshotForPersistence(runId, RunStatus.FAILED, startedAt, model, input, null, snapshot, null, error.message ?: "Local transcription failed."))
            }
        }.also(::registerForegroundCancellation)
    }

    private fun requireModel(): Boolean {
        if (mutableState.value.selectedModel?.installed == true) return true
        Log.w(TAG, "STT cannot start because no compatible model is selected.")
        mutableState.update { it.copy(errorMessage = UiText.Resource(CoreUiR.string.stt_error_install_model)) }
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

    private fun persistModelSelection(modelId: ModelId) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.updateSttSelectedModel(modelId.value)
        }
    }

    override fun onCleared() {
        Log.i(TAG, "STT ViewModel cleared; cancelling active work.")
        runtimeLeaseController.onHidden()
        super.onCleared()
    }

    private fun applyReplay(run: RunRecord) {
        Log.i(TAG, "STT replay configuration received: runId=${run.id}")
        val modelId = run.model?.modelId?.let(::ModelId)
        val selected = modelId?.takeIf { id ->
            mutableState.value.models.any { it.id == id && it.installed }
        }
        val parameters = runCatching { Json.parseToJsonElement(run.parametersJson).jsonObject }.getOrNull()
        mutableState.update { state ->
            state.copy(
                selectedModelId = selected ?: state.selectedModelId,
                language = parameters?.get("language")?.jsonPrimitive?.content?.let { code -> SttLanguage.entries.firstOrNull { it.whisperCode == code } } ?: state.language,
                threadCount = parameters?.get("threadCount")?.jsonPrimitive?.content ?: state.threadCount,
                errorMessage = when {
                    modelId != null && selected == null -> UiText.Resource(CoreUiR.string.stt_error_saved_model_missing, listOf(run.model?.displayName.orEmpty()))
                    else -> UiText.Resource(CoreUiR.string.stt_status_configuration_restored)
                },
            )
        }
        replayStore.consume(run.id)
    }

    private fun snapshotForPersistence(
        runId: String,
        status: RunStatus,
        startedAt: Long,
        model: SpeechModelOption?,
        input: PcmAudioInput,
        transcript: String?,
        state: SpeechToTextUiState,
        metrics: SpeechTranscriptionMetrics?,
        error: String?,
    ) = SttRunSnapshot(
        runId = runId,
        status = status,
        startedAtEpochMs = startedAt,
        model = model?.let { RunModelSnapshot(it.id.value, it.displayName, it.engineId.value) },
        inputDescription = "${input.displayName} (${input.durationMs} ms; ${input.sourceDescription})",
        transcript = transcript,
        languageCode = state.language.whisperCode,
        threadCount = state.threadCount,
        metrics = metrics,
        errorMessage = error,
    )

    private companion object {
        const val TAG = "AiP123Stt"
    }
}
