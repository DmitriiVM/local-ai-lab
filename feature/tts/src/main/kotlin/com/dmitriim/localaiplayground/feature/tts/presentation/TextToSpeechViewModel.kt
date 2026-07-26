package com.dmitriim.localaiplayground.feature.tts.presentation

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localaiplayground.core.audio.output.api.StreamingSpeechPlayer
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackStatus
import com.dmitriim.localaiplayground.core.audio.output.storage.GeneratedAudioStore
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelLibrary
import com.dmitriim.localaiplayground.core.model.AiCapability
import com.dmitriim.localaiplayground.core.model.RunModelSnapshot
import com.dmitriim.localaiplayground.core.model.RunRecord
import com.dmitriim.localaiplayground.core.model.RunStatus
import com.dmitriim.localaiplayground.source.runs.RunReplayStore
import com.dmitriim.localaiplayground.source.settings.AppSettingsRepository
import com.dmitriim.localaiplayground.core.result.ForegroundOperationCoordinator
import com.dmitriim.localaiplayground.feature.tts.domain.SpeechSynthesisEvent
import com.dmitriim.localaiplayground.feature.tts.domain.SpeechSynthesisRequest
import com.dmitriim.localaiplayground.feature.tts.domain.SpeechSynthesisSettings
import com.dmitriim.localaiplayground.feature.tts.domain.SynthesizeSpeech
import com.dmitriim.localaiplayground.feature.tts.domain.PersistTtsRun
import com.dmitriim.localaiplayground.feature.tts.domain.TtsRunSnapshot
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.floatOrNull

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class TextToSpeechViewModel(
    private val modelLibrary: ModelLibrary,
    private val synthesizeSpeech: SynthesizeSpeech,
    private val streamingSpeechPlayer: StreamingSpeechPlayer,
    private val generatedAudioStore: GeneratedAudioStore,
    private val operationCoordinator: ForegroundOperationCoordinator,
    private val persistTtsRun: PersistTtsRun,
    private val replayStore: RunReplayStore,
    private val settingsRepository: AppSettingsRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TextToSpeechUiState())
    val state: StateFlow<TextToSpeechUiState> = mutableState.asStateFlow()
    private var operationJob: Job? = null
    @Volatile private var hasTextInput = false

    init {
        viewModelScope.launch {
            settingsRepository.ttsDraft.first()?.let { savedText ->
                if (!hasTextInput) {
                    mutableState.update { it.copy(text = savedText.take(it.characterLimit)) }
                }
            }
        }
        viewModelScope.launch {
            modelLibrary.installedModels.collectLatest { installed ->
                val models = installed.filter { it.isReadyTtsModel() }.map { it.toTtsModelOption() }
                mutableState.update { current ->
                    val selected = current.selectedModelId
                        ?.takeIf { id -> models.any { it.id == id } }
                        ?: models.firstOrNull()?.id
                    current.copy(models = models, selectedModelId = selected)
                }
            }
        }
        viewModelScope.launch {
            streamingSpeechPlayer.state.collectLatest { playback ->
                mutableState.update { it.copy(playback = playback) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            generatedAudioStore.latest()?.let { output ->
                mutableState.update {
                    it.copy(
                        output = output,
                        statusMessage = "Latest generated WAV is retained until the next successful synthesis.",
                    )
                }
            }
        }
        viewModelScope.launch {
            replayStore.pending.collectLatest { run ->
                if (run?.capability == AiCapability.TEXT_TO_SPEECH) applyReplay(run)
            }
        }
    }

    fun selectModel(modelId: ModelId) {
        if (isActive()) return
        mutableState.update { it.copy(selectedModelId = modelId, errorMessage = null, metrics = null) }
    }

    fun updateText(value: String) {
        hasTextInput = true
        val text = value.take(mutableState.value.characterLimit)
        mutableState.update {
            it.copy(text = text, errorMessage = null, statusMessage = null)
        }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.updateTtsDraft(text)
        }
    }

    fun selectLanguage(language: TtsLanguage) {
        if (isActive()) return
        mutableState.update { it.copy(language = language, errorMessage = null) }
    }

    fun applySample(language: TtsLanguage) {
        if (isActive()) return
        mutableState.update { it.copy(language = language, text = language.sample, errorMessage = null) }
    }

    fun updateSpeed(value: Float) = mutableState.update { it.copy(speed = value, errorMessage = null) }

    fun updateSentenceSilence(value: Float) = mutableState.update {
        it.copy(sentenceSilenceScale = value, errorMessage = null)
    }

    fun updateVolume(value: Float) = mutableState.update { it.copy(volume = value, errorMessage = null) }

    fun updateThreadCount(value: String) = mutableState.update {
        it.copy(threadCount = value.filter(Char::isDigit), errorMessage = null)
    }

    fun synthesize() {
        if (isActive()) {
            Log.w(TAG, "Ignoring synthesis request because a TTS operation is already active.")
            return
        }
        val snapshot = mutableState.value
        val modelId = snapshot.selectedModelId ?: run {
            mutableState.update {
                it.copy(errorMessage = "Install Supertonic 3 INT8 before synthesizing speech.")
            }
            return
        }
        val threads = snapshot.threadCount.toIntOrNull() ?: run {
            mutableState.update { it.copy(errorMessage = "Thread count must be a whole number.") }
            return
        }
        val startedAt = System.currentTimeMillis()
        val model = snapshot.selectedModel
        Log.i(
            TAG,
            "TTS UI synthesis started: model=${model?.displayName}, textLength=${snapshot.text.length}, " +
                "language=${snapshot.language.code}, speaker=${snapshot.speakerId}, speed=${snapshot.speed}, " +
                "silenceScale=${snapshot.sentenceSilenceScale}, volume=${snapshot.volume}, threads=$threads",
        )
        mutableState.update {
            it.copy(
                operation = TtsOperation.SYNTHESIZING,
                metrics = null,
                errorMessage = null,
                statusMessage = "Synthesizing and streaming PCM locally…",
            )
        }
        operationJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                synthesizeSpeech.execute(
                    SpeechSynthesisRequest(
                        modelId = modelId,
                        text = snapshot.text,
                        settings = SpeechSynthesisSettings(
                            languageCode = snapshot.language.code,
                            speakerId = snapshot.speakerId,
                            speed = snapshot.speed,
                            sentenceSilenceScale = snapshot.sentenceSilenceScale,
                            volume = snapshot.volume,
                            threadCount = threads,
                        ),
                    ),
                ).collect { event ->
                    when (event) {
                        is SpeechSynthesisEvent.Prepared -> {
                            Log.i(TAG, "TTS UI received prepared event: loadMs=${event.loadDurationMs}, sampleRateHz=${event.sampleRateHz}, speakers=${event.speakerCount}")
                            mutableState.update { it.copy(speakerCount = event.speakerCount) }
                        }
                        is SpeechSynthesisEvent.Synthesized -> {
                            Log.i(TAG, "TTS UI received synthesized event: durationMs=${event.synthesisDurationMs}, outputDurationMs=${event.output.durationMs}")
                            mutableState.update {
                                it.copy(
                                    operation = TtsOperation.IDLE,
                                    output = event.output,
                                    statusMessage = "Synthesis completed; playback is draining by presented frames.",
                                )
                            }
                        }
                        is SpeechSynthesisEvent.Completed -> mutableState.update {
                            it.copy(
                                operation = TtsOperation.IDLE,
                                output = event.output,
                                metrics = event.metrics,
                                statusMessage = "Latest WAV retained in app-private storage until the next successful synthesis.",
                            )
                        }.also {
                            Log.i(TAG, "TTS UI received completed event: synthesisMs=${event.metrics.synthesisDurationMs}, underruns=${event.metrics.playbackUnderrunCount}")
                            persistTtsRun(snapshotForPersistence(RunStatus.SUCCEEDED, startedAt, model, snapshot, event.metrics, null))
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                Log.i(TAG, "TTS UI operation cancelled.")
                mutableState.update {
                    it.copy(operation = TtsOperation.IDLE, statusMessage = "Speech operation stopped.")
                }
                persistTtsRun(snapshotForPersistence(RunStatus.CANCELLED, startedAt, model, snapshot, null, "Speech operation stopped."))
            } catch (error: Throwable) {
                Log.e(TAG, "TTS UI operation failed: ${error.message}", error)
                mutableState.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        errorMessage = error.message ?: "Local speech synthesis failed.",
                        statusMessage = null,
                    )
                }
                persistTtsRun(snapshotForPersistence(RunStatus.FAILED, startedAt, model, snapshot, null, error.message ?: "Local speech synthesis failed."))
            }
        }.also(::registerForegroundCancellation)
    }

    fun pausePlayback() {
        Log.i(TAG, "TTS UI pause requested.")
        synthesizeSpeech.pausePlayback()
    }

    fun resumePlayback() {
        Log.i(TAG, "TTS UI resume requested.")
        synthesizeSpeech.resumePlayback()
    }

    fun stop() {
        if (!isActive() && mutableState.value.playback.status !in activePlaybackStatuses) return
        Log.i(TAG, "TTS UI stop requested: operation=${mutableState.value.operation}, playback=${mutableState.value.playback.status}")
        mutableState.update { it.copy(operation = TtsOperation.CANCELLING) }
        synthesizeSpeech.cancel()
        operationJob?.cancel()
    }

    fun replay() {
        if (isActive()) {
            Log.w(TAG, "Ignoring replay request because a TTS operation is already active.")
            return
        }
        val output = mutableState.value.output ?: return
        val volume = mutableState.value.volume
        Log.i(TAG, "TTS UI replay started: durationMs=${output.durationMs}, volume=$volume")
        mutableState.update { it.copy(errorMessage = null, statusMessage = "Replaying the retained WAV.") }
        operationJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                synthesizeSpeech.replay(output, volume)
                Log.i(TAG, "TTS UI replay completed.")
                mutableState.update { it.copy(statusMessage = "Replay completed.") }
            } catch (cancelled: CancellationException) {
                Log.i(TAG, "TTS UI replay cancelled.")
                mutableState.update { it.copy(statusMessage = "Playback stopped.") }
            } catch (error: Throwable) {
                Log.e(TAG, "TTS UI replay failed: ${error.message}", error)
                mutableState.update {
                    it.copy(errorMessage = error.message ?: "Could not replay generated speech.")
                }
            }
        }.also(::registerForegroundCancellation)
    }

    fun export(destination: Uri) {
        val output = mutableState.value.output ?: return
        Log.i(TAG, "TTS UI export started: destinationScheme=${destination.scheme}, durationMs=${output.durationMs}")
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { generatedAudioStore.export(output, destination) }
                .onSuccess {
                    Log.i(TAG, "TTS UI export completed.")
                    mutableState.update {
                        it.copy(statusMessage = "WAV exported successfully.", errorMessage = null)
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "TTS UI export failed: ${error.message}", error)
                    mutableState.update {
                        it.copy(errorMessage = error.message ?: "Could not export the WAV file.")
                    }
                }
        }
    }

    fun shareFailed(message: String) = mutableState.update { it.copy(errorMessage = message) }

    private fun isActive(): Boolean = operationJob?.isActive == true

    private fun registerForegroundCancellation(job: Job) {
        val registration = operationCoordinator.register(::stop)
        job.invokeOnCompletion { registration.close() }
    }

    override fun onCleared() {
        Log.i(TAG, "TTS ViewModel cleared; cancelling active work.")
        synthesizeSpeech.cancel()
        operationJob?.cancel()
        super.onCleared()
    }

    private fun applyReplay(run: RunRecord) {
        val modelId = run.model?.modelId?.let(::ModelId)
        val selected = modelId?.takeIf { id -> mutableState.value.models.any { it.id == id } }
        val parameters = runCatching { Json.parseToJsonElement(run.parametersJson).jsonObject }.getOrNull()
        mutableState.update { state ->
            state.copy(
                selectedModelId = selected ?: state.selectedModelId,
                text = run.input?.take(state.characterLimit) ?: state.text,
                language = parameters?.get("language")?.jsonPrimitive?.content?.let { code -> TtsLanguage.entries.firstOrNull { it.code == code } } ?: state.language,
                speed = parameters?.get("speed")?.jsonPrimitive?.floatOrNull ?: state.speed,
                sentenceSilenceScale = parameters?.get("sentenceSilenceScale")?.jsonPrimitive?.floatOrNull ?: state.sentenceSilenceScale,
                volume = parameters?.get("volume")?.jsonPrimitive?.floatOrNull ?: state.volume,
                threadCount = parameters?.get("threadCount")?.jsonPrimitive?.content ?: state.threadCount,
                errorMessage = if (modelId != null && selected == null) "Saved model ${run.model?.displayName.orEmpty()} is no longer installed. Select a compatible model before synthesizing." else null,
            )
        }
        replayStore.consume(run.id)
    }

    private companion object {
        const val TAG = "AiP123Tts"
        val activePlaybackStatuses = setOf(
            SpeechPlaybackStatus.READY,
            SpeechPlaybackStatus.PLAYING,
            SpeechPlaybackStatus.PAUSED,
        )
    }

    private fun snapshotForPersistence(
        status: RunStatus,
        startedAt: Long,
        model: TtsModelOption?,
        snapshot: TextToSpeechUiState,
        metrics: com.dmitriim.localaiplayground.feature.tts.domain.SpeechSynthesisMetrics?,
        error: String?,
    ) = TtsRunSnapshot(
        status = status,
        startedAtEpochMs = startedAt,
        model = model?.let { RunModelSnapshot(it.id.value, it.displayName, "sherpa-onnx") },
        input = snapshot.text,
        languageCode = snapshot.language.code,
        speakerId = snapshot.speakerId,
        speed = snapshot.speed,
        sentenceSilenceScale = snapshot.sentenceSilenceScale,
        volume = snapshot.volume,
        threadCount = snapshot.threadCount,
        metrics = metrics,
        errorMessage = error,
    )
}
