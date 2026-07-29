package com.dmitriim.localaiplayground.feature.tts.presentation

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localaiplayground.core.audio.output.api.StreamingSpeechPlayer
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackStatus
import com.dmitriim.localaiplayground.core.audio.output.storage.GeneratedAudioStore
import com.dmitriim.localaiplayground.core.audio.processing.SpeechAudioEffects
import com.dmitriim.localaiplayground.core.audio.input.storage.ReferenceVoiceStore
import com.dmitriim.localaiplayground.ai.api.TextToSpeechVoiceCondition
import com.dmitriim.localaiplayground.core.model.TtsVoiceMode
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelLibrary
import com.dmitriim.localaiplayground.core.model.ModelTransfers
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
import com.dmitriim.localaiplayground.feature.tts.domain.PreviewSpeech
import com.dmitriim.localaiplayground.feature.tts.domain.SpeechPreviewRequest
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class TextToSpeechViewModel(
    private val modelLibrary: ModelLibrary,
    private val modelTransfers: ModelTransfers,
    private val synthesizeSpeech: SynthesizeSpeech,
    private val previewSpeech: PreviewSpeech,
    private val streamingSpeechPlayer: StreamingSpeechPlayer,
    private val generatedAudioStore: GeneratedAudioStore,
    private val operationCoordinator: ForegroundOperationCoordinator,
    private val persistTtsRun: PersistTtsRun,
    private val replayStore: RunReplayStore,
    private val settingsRepository: AppSettingsRepository,
    private val referenceVoiceStore: ReferenceVoiceStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TextToSpeechUiState())
    val state: StateFlow<TextToSpeechUiState> = mutableState.asStateFlow()
    private var operationJob: Job? = null
    private val savedVoiceIds = mutableMapOf<String, String>()
    private var savedModelId: ModelId? = null
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
            val selection = settingsRepository.ttsSelection.first()
            savedModelId = selection.selectedModelId?.let(::ModelId)
            savedVoiceIds.putAll(selection.voiceIdsByModel)
            combine(
                modelLibrary.installedModels,
                modelTransfers.catalog,
                referenceVoiceStore.voices,
            ) { installed, catalog, references ->
                installed.filter { it.isReadyTtsModel() }.map { it.toTtsModelOption(catalog) } to references
            }.collectLatest { (models, references) ->
                mutableState.update { current ->
                    val selected = current.selectedModelId
                        ?.takeIf { id -> models.any { it.id == id } }
                        ?: savedModelId?.takeIf { id -> models.any { it.id == id } }
                        ?: models.firstOrNull()?.id
                    val selectedModel = models.firstOrNull { it.id == selected }
                    val language = if (selectedModel?.voiceMode == TtsVoiceMode.REFERENCE_AUDIO) {
                        TtsLanguage.ENGLISH
                    } else current.language
                    val voices = voicesFor(selectedModel, language, references)
                    val preferredVoiceId = current.selectedVoiceId
                        ?.takeIf { current.selectedModelId == selected }
                        ?: selected?.value?.let(savedVoiceIds::get)
                    current.copy(
                        models = models,
                        referenceVoices = references,
                        selectedModelId = selected,
                        language = language,
                        selectedVoiceId = preferredVoiceId
                            ?.takeIf { id -> voices.any { it.id == id } }
                            ?: voices.firstOrNull()?.id,
                    )
                }
                mutableState.value.selectedModelId?.let { selectedModelId ->
                    if (savedModelId != selectedModelId) {
                        savedModelId = selectedModelId
                        persistModelSelection(selectedModelId)
                    }
                    mutableState.value.selectedVoice?.let { selectedVoice ->
                        if (savedVoiceIds[selectedModelId.value] != selectedVoice.id) {
                            persistVoiceSelection(selectedVoice)
                        }
                    }
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
        if (mutableState.value.selectedModelId != modelId) synthesizeSpeech.unloadRuntime()
        mutableState.update { state ->
            val model = state.models.firstOrNull { it.id == modelId } ?: return@update state
            val language = if (model.voiceMode == TtsVoiceMode.REFERENCE_AUDIO) {
                TtsLanguage.ENGLISH
            } else state.language
            val voices = voicesFor(model, language, state.referenceVoices)
            val selectedVoiceId = savedVoiceIds[modelId.value]
                ?.takeIf { id -> voices.any { it.id == id } }
                ?: voices.firstOrNull()?.id
            state.copy(
                selectedModelId = modelId,
                language = language,
                selectedVoiceId = selectedVoiceId,
                errorMessage = null,
                metrics = null,
            )
        }
        savedModelId = modelId
        persistModelSelection(modelId)
        mutableState.value.selectedVoice?.let(::persistVoiceSelection)
    }

    fun selectVoice(voiceId: String) {
        if (isActive()) return
        val state = mutableState.value
        val voice = state.compatibleVoices.firstOrNull { it.id == voiceId } ?: return
        mutableState.update {
            it.copy(selectedVoiceId = voice.id, errorMessage = null, metrics = null)
        }
        persistVoiceSelection(voice)
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
        if (mutableState.value.usesReferenceVoice && language != TtsLanguage.ENGLISH) return
        mutableState.update { state ->
            val voices = voicesFor(state.selectedModel, language, state.referenceVoices)
            state.copy(
                language = language,
                selectedVoiceId = state.selectedVoiceId
                    ?.takeIf { id -> voices.any { it.id == id } }
                    ?: state.selectedModelId?.value
                        ?.let(savedVoiceIds::get)
                        ?.takeIf { id -> voices.any { it.id == id } }
                    ?: voices.firstOrNull()?.id,
                errorMessage = null,
            )
        }
        mutableState.value.selectedVoice?.let(::persistVoiceSelection)
    }

    fun applySample(language: TtsLanguage) {
        if (isActive()) return
        mutableState.update { state ->
            if (state.usesReferenceVoice && language != TtsLanguage.ENGLISH) return@update state
            val voices = voicesFor(state.selectedModel, language, state.referenceVoices)
            state.copy(
                language = language,
                text = language.sample,
                selectedVoiceId = state.selectedVoiceId
                    ?.takeIf { id -> voices.any { it.id == id } }
                    ?: state.selectedModelId?.value
                        ?.let(savedVoiceIds::get)
                        ?.takeIf { id -> voices.any { it.id == id } }
                    ?: voices.firstOrNull()?.id,
                errorMessage = null,
            )
        }
        mutableState.value.selectedVoice?.let(::persistVoiceSelection)
    }

    fun updateSpeed(value: Float) = mutableState.update { it.copy(speed = value, errorMessage = null) }

    fun updateSentenceSilence(value: Float) = mutableState.update {
        it.copy(sentenceSilenceScale = value, errorMessage = null)
    }

    fun updateVolume(value: Float) = mutableState.update { it.copy(volume = value, errorMessage = null) }

    fun updatePitch(value: Float) = mutableState.update {
        it.copy(
            audioEffects = it.audioEffects.copy(pitchSemitones = value),
            errorMessage = null,
        )
    }

    fun updateFormant(value: Float) = mutableState.update {
        it.copy(
            audioEffects = it.audioEffects.copy(formantSemitones = value),
            errorMessage = null,
        )
    }

    fun updateLowEq(value: Float) = mutableState.update {
        it.copy(audioEffects = it.audioEffects.copy(lowEqDb = value), errorMessage = null)
    }

    fun updateMidEq(value: Float) = mutableState.update {
        it.copy(audioEffects = it.audioEffects.copy(midEqDb = value), errorMessage = null)
    }

    fun updateHighEq(value: Float) = mutableState.update {
        it.copy(audioEffects = it.audioEffects.copy(highEqDb = value), errorMessage = null)
    }

    fun updateSaturation(value: Float) = mutableState.update {
        it.copy(
            audioEffects = it.audioEffects.copy(saturationDriveDb = value),
            errorMessage = null,
        )
    }

    fun resetAudioEffects() = mutableState.update {
        it.copy(
            audioEffects = SpeechAudioEffects(),
            errorMessage = null,
        )
    }

    fun updateThreadCount(value: String) = mutableState.update {
        it.copy(threadCount = value.filter(Char::isDigit), errorMessage = null)
    }

    fun startReferenceRecording() {
        if (isActive() || !mutableState.value.usesReferenceVoice) return
        mutableState.update {
            it.copy(
                operation = TtsOperation.RECORDING_REFERENCE,
                referenceLevel = null,
                errorMessage = null,
                statusMessage = "Record 5–10 seconds of clear speech. Recording stops automatically at 10 seconds.",
            )
        }
        operationJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val voice = referenceVoiceStore.capture { level ->
                    mutableState.update { it.copy(referenceLevel = level) }
                }
                mutableState.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        referenceLevel = null,
                        referenceVoices = referenceVoiceStore.voices.value,
                        selectedVoiceId = voice.id,
                        statusMessage = "Saved reference voice “${voice.displayName}”.",
                    )
                }
                persistVoiceSelection(requireNotNull(mutableState.value.selectedVoice))
            } catch (cancelled: CancellationException) {
                mutableState.update {
                    it.copy(operation = TtsOperation.IDLE, referenceLevel = null, statusMessage = "Reference recording stopped.")
                }
            } catch (error: Throwable) {
                mutableState.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        referenceVoices = referenceVoiceStore.voices.value,
                        referenceLevel = null,
                        errorMessage = error.message ?: "Could not record the reference voice.",
                        statusMessage = null,
                    )
                }
            }
        }.also(::registerForegroundCancellation)
    }

    fun stopReferenceRecording() {
        if (mutableState.value.operation != TtsOperation.RECORDING_REFERENCE) return
        mutableState.update { it.copy(operation = TtsOperation.STOPPING_REFERENCE) }
        referenceVoiceStore.stopCapture()
    }

    fun importReferenceAudio(uri: Uri) {
        if (isActive() || !mutableState.value.usesReferenceVoice) return
        mutableState.update {
            it.copy(
                operation = TtsOperation.IMPORTING_REFERENCE,
                errorMessage = null,
                statusMessage = "Normalizing reference audio to mono 24 kHz…",
            )
        }
        operationJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val voice = referenceVoiceStore.importAudio(uri)
                mutableState.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        referenceVoices = referenceVoiceStore.voices.value,
                        selectedVoiceId = voice.id,
                        statusMessage = "Saved reference voice “${voice.displayName}”.",
                    )
                }
                persistVoiceSelection(requireNotNull(mutableState.value.selectedVoice))
            } catch (cancelled: CancellationException) {
                mutableState.update { it.copy(operation = TtsOperation.IDLE) }
            } catch (error: Throwable) {
                mutableState.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        errorMessage = error.message ?: "Could not import the reference audio.",
                        statusMessage = null,
                    )
                }
            }
        }.also(::registerForegroundCancellation)
    }

    fun deleteReferenceVoice(voiceId: String) {
        if (isActive()) return
        referenceVoiceStore.delete(voiceId)
        val selectedModelId = mutableState.value.selectedModelId
        if (selectedModelId != null && savedVoiceIds[selectedModelId.value] == voiceId) {
            savedVoiceIds.remove(selectedModelId.value)
            viewModelScope.launch(Dispatchers.IO) {
                settingsRepository.clearTtsVoice(selectedModelId.value)
            }
        }
        mutableState.update { state ->
            state.copy(
                selectedVoiceId = state.selectedVoiceId.takeUnless { it == voiceId },
                errorMessage = if (state.selectedVoiceId == voiceId) {
                    "The selected reference was deleted. Record or import another voice."
                } else state.errorMessage,
            )
        }
    }

    fun microphonePermissionDenied() = mutableState.update {
        it.copy(errorMessage = "Microphone permission was denied. Allow it in Android settings and try again.")
    }

    fun previewVoice(voiceId: String) {
        val snapshot = mutableState.value
        val activeJob = operationJob?.takeIf(Job::isActive)
        if (activeJob != null && snapshot.operation != TtsOperation.PREVIEWING) return
        if (
            snapshot.operation == TtsOperation.PREVIEWING &&
            snapshot.previewVoiceId == voiceId
        ) {
            stop()
            return
        }
        val modelId = snapshot.selectedModelId ?: return
        val voice = snapshot.compatibleVoices.firstOrNull { it.id == voiceId } ?: run {
            mutableState.update {
                it.copy(errorMessage = "That voice does not support ${snapshot.language.label}.")
            }
            return
        }
        if (snapshot.text.isBlank()) {
            mutableState.update {
                it.copy(errorMessage = "Enter text before previewing a voice.")
            }
            return
        }
        val threads = snapshot.threadCount.toIntOrNull() ?: run {
            mutableState.update { it.copy(errorMessage = "Thread count must be a whole number.") }
            return
        }
        if (activeJob != null) {
            mutableState.update {
                it.copy(
                    operation = TtsOperation.CANCELLING,
                    previewVoiceId = voice.id,
                    statusMessage = "Switching preview to ${voice.displayName}…",
                )
            }
            previewSpeech.cancel()
            activeJob.cancel()
        }
        operationJob = viewModelScope.launch(Dispatchers.Default) {
            activeJob?.join()
            mutableState.update {
                it.copy(
                    operation = TtsOperation.PREVIEWING,
                    previewVoiceId = voice.id,
                    errorMessage = null,
                    statusMessage = "Previewing ${voice.displayName} locally…",
                )
            }
            try {
                previewSpeech.execute(
                    SpeechPreviewRequest(
                        modelId = modelId,
                        text = snapshot.text,
                        voiceName = voice.displayName,
                        settings = SpeechSynthesisSettings(
                            languageCode = snapshot.language.code,
                            voiceCondition = voiceCondition(voice),
                            voiceName = voice.displayName,
                            expectedSpeakerCount = snapshot.selectedModel?.speakerCount,
                            speed = snapshot.speed,
                            sentenceSilenceScale = snapshot.sentenceSilenceScale,
                            volume = snapshot.volume,
                            threadCount = threads,
                            audioEffects = snapshot.audioEffects,
                        ),
                    ),
                )
                mutableState.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        previewVoiceId = null,
                        statusMessage = "Voice preview completed.",
                    )
                }
            } catch (cancelled: CancellationException) {
                mutableState.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        previewVoiceId = null,
                        statusMessage = "Voice preview stopped.",
                    )
                }
            } catch (error: Throwable) {
                Log.e(TAG, "TTS voice preview failed: ${error.message}", error)
                mutableState.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        previewVoiceId = null,
                        errorMessage = error.message ?: "Could not preview the selected voice.",
                        statusMessage = null,
                    )
                }
            }
        }.also(::registerForegroundCancellation)
    }

    fun synthesize() {
        if (isActive()) {
            Log.w(TAG, "Ignoring synthesis request because a TTS operation is already active.")
            return
        }
        val snapshot = mutableState.value
        val modelId = snapshot.selectedModelId ?: run {
            mutableState.update {
                it.copy(errorMessage = "Install a compatible voice model before synthesizing speech.")
            }
            return
        }
        val voice = snapshot.selectedVoice ?: run {
            mutableState.update {
                it.copy(errorMessage = "Select a voice that supports ${snapshot.language.label}.")
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
                "language=${snapshot.language.code}, voice=${voice.id}, speaker=${voice.speakerId}, speed=${snapshot.speed}, " +
                "silenceScale=${snapshot.sentenceSilenceScale}, volume=${snapshot.volume}, threads=$threads, " +
                "audioEffects=${snapshot.audioEffects}",
        )
        mutableState.update {
            it.copy(
                operation = TtsOperation.SYNTHESIZING,
                previewVoiceId = null,
                metrics = null,
                errorMessage = null,
                statusMessage = if (snapshot.usesReferenceVoice) {
                    "Generating Chatterbox speech locally…"
                } else {
                    "Synthesizing and streaming PCM locally…"
                },
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
                            voiceCondition = voiceCondition(voice),
                            voiceName = voice.displayName,
                            expectedSpeakerCount = snapshot.selectedModel?.speakerCount,
                            speed = snapshot.speed,
                            sentenceSilenceScale = snapshot.sentenceSilenceScale,
                            volume = snapshot.volume,
                            threadCount = threads,
                            audioEffects = snapshot.audioEffects,
                        ),
                    ),
                ).collect { event ->
                    when (event) {
                        is SpeechSynthesisEvent.Prepared -> {
                            Log.i(TAG, "TTS UI received prepared event: loadMs=${event.loadDurationMs}, sampleRateHz=${event.sampleRateHz}, speakers=${event.speakerCount}")
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
        referenceVoiceStore.stopCapture()
        previewSpeech.cancel()
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
        previewSpeech.cancel()
        synthesizeSpeech.cancel()
        referenceVoiceStore.stopCapture()
        operationJob?.cancel()
        synthesizeSpeech.unloadRuntime()
        super.onCleared()
    }

    private fun applyReplay(run: RunRecord) {
        val modelId = run.model?.modelId?.let(::ModelId)
        val selected = modelId?.takeIf { id -> mutableState.value.models.any { it.id == id } }
        val parameters = runCatching { Json.parseToJsonElement(run.parametersJson).jsonObject }.getOrNull()
        val replayLanguage = parameters?.get("language")?.jsonPrimitive?.content
            ?.let { code -> TtsLanguage.entries.firstOrNull { it.code == code } }
        val replayVoiceId = parameters?.get("voiceId")?.jsonPrimitive?.content
        val replaySpeakerId = parameters?.get("speakerId")?.jsonPrimitive?.intOrNull
        mutableState.update { state ->
            val language = if (selectedModelForReplay(state, selected)?.voiceMode == TtsVoiceMode.REFERENCE_AUDIO) {
                TtsLanguage.ENGLISH
            } else replayLanguage ?: state.language
            val selectedModelId = selected ?: state.selectedModelId
            val selectedModel = state.models.firstOrNull { it.id == selectedModelId }
            val voices = voicesFor(selectedModel, language, state.referenceVoices)
            val selectedVoiceId = if (modelId == null || selected != null) {
                replayVoiceId
                    ?.takeIf { id -> voices.any { it.id == id } }
                    ?: replaySpeakerId
                        ?.let { speakerId -> voices.firstOrNull { it.speakerId == speakerId }?.id }
                    ?: state.selectedVoiceId?.takeIf { id -> voices.any { it.id == id } }
                    ?: voices.firstOrNull()?.id
            } else {
                state.selectedVoiceId?.takeIf { id -> voices.any { it.id == id } }
                    ?: voices.firstOrNull()?.id
            }
            state.copy(
                selectedModelId = selectedModelId,
                selectedVoiceId = selectedVoiceId,
                text = run.input?.take(state.characterLimit) ?: state.text,
                language = language,
                speed = parameters?.get("speed")?.jsonPrimitive?.floatOrNull ?: state.speed,
                sentenceSilenceScale = parameters?.get("sentenceSilenceScale")?.jsonPrimitive?.floatOrNull ?: state.sentenceSilenceScale,
                volume = parameters?.get("volume")?.jsonPrimitive?.floatOrNull ?: state.volume,
                threadCount = parameters?.get("threadCount")?.jsonPrimitive?.content ?: state.threadCount,
                audioEffects = state.audioEffects.copy(
                    pitchSemitones = parameters?.get("pitchSemitones")?.jsonPrimitive?.floatOrNull
                        ?: state.audioEffects.pitchSemitones,
                    formantSemitones = parameters?.get("formantSemitones")?.jsonPrimitive?.floatOrNull
                        ?: state.audioEffects.formantSemitones,
                    lowEqDb = parameters?.get("lowEqDb")?.jsonPrimitive?.floatOrNull
                        ?: state.audioEffects.lowEqDb,
                    midEqDb = parameters?.get("midEqDb")?.jsonPrimitive?.floatOrNull
                        ?: state.audioEffects.midEqDb,
                    highEqDb = parameters?.get("highEqDb")?.jsonPrimitive?.floatOrNull
                        ?: state.audioEffects.highEqDb,
                    saturationDriveDb = parameters?.get("saturationDriveDb")?.jsonPrimitive?.floatOrNull
                        ?: state.audioEffects.saturationDriveDb,
                ),
                errorMessage = when {
                    modelId != null && selected == null ->
                        "Saved model ${run.model?.displayName.orEmpty()} is no longer installed. Select a compatible model before synthesizing."
                    selectedModel?.voiceMode == TtsVoiceMode.REFERENCE_AUDIO &&
                        replayVoiceId != null &&
                        voices.none { it.id == replayVoiceId } ->
                        "The saved reference voice was deleted. Record or import another reference before synthesizing."
                    else -> null
                },
            )
        }
        if (selected != null) {
            savedModelId = selected
            persistModelSelection(selected)
            mutableState.value.selectedVoice?.let(::persistVoiceSelection)
        }
        replayStore.consume(run.id)
    }

    private fun persistModelSelection(modelId: ModelId) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.updateTtsSelectedModel(modelId.value)
        }
    }

    private fun persistVoiceSelection(voice: TtsVoiceOption) {
        val modelId = mutableState.value.selectedModelId ?: return
        savedVoiceIds[modelId.value] = voice.id
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.updateTtsVoice(modelId.value, voice.id)
        }
    }

    private fun voiceCondition(voice: TtsVoiceOption): TextToSpeechVoiceCondition =
        voice.reference?.let { reference ->
            TextToSpeechVoiceCondition.ReferenceAudio(
                referenceId = reference.id,
                displayName = reference.displayName,
                pcmFilePath = reference.pcmFilePath,
                sampleRateHz = reference.sampleRateHz,
            )
        } ?: TextToSpeechVoiceCondition.FixedSpeaker(
            requireNotNull(voice.speakerId) { "The selected fixed voice has no speaker ID." },
        )

    private fun voicesFor(
        model: TtsModelOption?,
        language: TtsLanguage,
        references: List<com.dmitriim.localaiplayground.core.audio.input.storage.ReferenceVoice>,
    ): List<TtsVoiceOption> = if (model?.voiceMode == TtsVoiceMode.REFERENCE_AUDIO) {
        references.map { reference ->
            TtsVoiceOption(
                id = reference.id,
                displayName = reference.displayName,
                speakerId = null,
                languages = setOf("en"),
                description = "${reference.durationMs / 1_000.0} s · ${reference.sourceDescription}",
                reference = reference,
            )
        }
    } else {
        model?.compatibleVoices(language).orEmpty()
    }

    private fun selectedModelForReplay(state: TextToSpeechUiState, selected: ModelId?) =
        state.models.firstOrNull { it.id == (selected ?: state.selectedModelId) }

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
        model = model?.let { RunModelSnapshot(it.id.value, it.displayName, it.engineId.value) },
        input = snapshot.text,
        languageCode = snapshot.language.code,
        voiceId = requireNotNull(snapshot.selectedVoice).id,
        voiceName = snapshot.selectedVoice!!.displayName,
        speakerId = snapshot.selectedVoice!!.speakerId,
        referenceVoiceId = snapshot.selectedVoice!!.reference?.id,
        referenceVoiceName = snapshot.selectedVoice!!.reference?.displayName,
        watermarkStatus = if (snapshot.usesReferenceVoice) "NOT_WATERMARKED" else "NOT_APPLICABLE",
        speed = snapshot.speed,
        sentenceSilenceScale = snapshot.sentenceSilenceScale,
        volume = snapshot.volume,
        threadCount = snapshot.threadCount,
        audioEffects = snapshot.audioEffects,
        metrics = metrics,
        errorMessage = error,
    )
}
