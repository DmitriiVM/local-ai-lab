package com.dmitriim.localaiplayground.feature.tts.presentation

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localaiplayground.ai.api.system.SystemTextToSpeechSupport
import com.dmitriim.localaiplayground.core.audio.input.storage.ReferenceVoiceStore
import com.dmitriim.localaiplayground.core.audio.output.api.StreamingSpeechPlayer
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackStatus
import com.dmitriim.localaiplayground.core.audio.output.storage.GeneratedAudioStore
import com.dmitriim.localaiplayground.core.audio.processing.SpeechAudioEffects
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.model.manifest.TtsVoiceMode
import com.dmitriim.localaiplayground.core.model.runs.RunRecord
import com.dmitriim.localaiplayground.core.model.service.ModelLibrary
import com.dmitriim.localaiplayground.core.model.service.ModelTransfers
import com.dmitriim.localaiplayground.core.performance.BenchmarkWorkload
import com.dmitriim.localaiplayground.core.performance.ProfileLaunchCoordinator
import com.dmitriim.localaiplayground.core.result.ForegroundOperationCoordinator
import com.dmitriim.localaiplayground.core.voice.tts.PreviewSpeech
import com.dmitriim.localaiplayground.core.voice.tts.SynthesizeSpeech
import com.dmitriim.localaiplayground.feature.tts.domain.PersistTtsRun
import com.dmitriim.localaiplayground.source.runs.RunReplayStore
import com.dmitriim.localaiplayground.source.settings.AppSettingsRepository
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class TextToSpeechViewModel(
    private val modelLibrary: ModelLibrary,
    private val modelTransfers: ModelTransfers,
    synthesizeSpeech: SynthesizeSpeech,
    previewSpeech: PreviewSpeech,
    private val streamingSpeechPlayer: StreamingSpeechPlayer,
    private val generatedAudioStore: GeneratedAudioStore,
    operationCoordinator: ForegroundOperationCoordinator,
    persistTtsRun: PersistTtsRun,
    private val replayStore: RunReplayStore,
    private val settingsRepository: AppSettingsRepository,
    private val referenceVoiceStore: ReferenceVoiceStore,
    private val systemTextToSpeechSupport: SystemTextToSpeechSupport,
    private val profileLaunchCoordinator: ProfileLaunchCoordinator,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TextToSpeechUiState())
    val state: StateFlow<TextToSpeechUiState> = mutableState.asStateFlow()
    private val savedVoiceIds = mutableMapOf<String, String>()
    private var savedModelId: ModelId? = null

    @Volatile private var hasTextInput = false

    private val operationController = TextToSpeechOperationController(
        scope = viewModelScope,
        state = mutableState,
        synthesizeSpeech = synthesizeSpeech,
        previewSpeech = previewSpeech,
        referenceVoiceStore = referenceVoiceStore,
        operationCoordinator = operationCoordinator,
        persistTtsRun = persistTtsRun,
        onVoiceSelected = ::persistVoiceSelection,
    )

    init {
        refreshSystemVoices()
        restoreDraft()
        observeAvailableVoices()
        observePlayback()
        restoreLatestOutput()
        observeReplayRequests()
    }

    fun selectModel(modelId: ModelId) {
        if (operationController.isActive()) return
        if (mutableState.value.models.none { it.id == modelId && it.installed }) return
        if (mutableState.value.selectedModelId != modelId) operationController.unloadRuntime()
        mutableState.update { state ->
            val model = state.models.firstOrNull { it.id == modelId } ?: return@update state
            val language = if (model.voiceMode == TtsVoiceMode.REFERENCE_AUDIO) {
                TtsLanguage.ENGLISH
            } else {
                state.language
            }
            val voices = TtsVoiceResolver.forModel(model, language, state.referenceVoices)
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
        if (operationController.isActive()) return
        val voice = mutableState.value.compatibleVoices.firstOrNull { it.id == voiceId } ?: return
        mutableState.update { it.copy(selectedVoiceId = voice.id, errorMessage = null, metrics = null) }
        persistVoiceSelection(voice)
    }

    fun updateText(value: String) {
        hasTextInput = true
        val text = value.take(mutableState.value.characterLimit)
        mutableState.update { it.copy(text = text, errorMessage = null, statusMessage = null) }
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.updateTtsDraft(text) }
    }

    fun selectLanguage(language: TtsLanguage) {
        if (operationController.isActive()) return
        if (mutableState.value.usesReferenceVoice && language != TtsLanguage.ENGLISH) return
        mutableState.update { state ->
            val voices = TtsVoiceResolver.forModel(state.selectedModel, language, state.referenceVoices)
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
        if (operationController.isActive()) return
        mutableState.update { state ->
            if (state.usesReferenceVoice && language != TtsLanguage.ENGLISH) return@update state
            val voices = TtsVoiceResolver.forModel(state.selectedModel, language, state.referenceVoices)
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
        it.copy(audioEffects = it.audioEffects.copy(pitchSemitones = value), errorMessage = null)
    }

    fun updateFormant(value: Float) = mutableState.update {
        it.copy(audioEffects = it.audioEffects.copy(formantSemitones = value), errorMessage = null)
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
        it.copy(audioEffects = it.audioEffects.copy(saturationDriveDb = value), errorMessage = null)
    }

    fun resetAudioEffects() = mutableState.update {
        it.copy(audioEffects = SpeechAudioEffects(), errorMessage = null)
    }

    fun updateThreadCount(value: String) = mutableState.update {
        it.copy(threadCount = value.filter(Char::isDigit), errorMessage = null)
    }

    fun startReferenceRecording() = operationController.startReferenceRecording()

    fun stopReferenceRecording() = operationController.stopReferenceRecording()

    fun importReferenceAudio(uri: Uri) = operationController.importReferenceAudio(uri)

    fun deleteReferenceVoice(voiceId: String) {
        if (operationController.isActive()) return
        referenceVoiceStore.delete(voiceId)
        val selectedModelId = mutableState.value.selectedModelId
        if (selectedModelId != null && savedVoiceIds[selectedModelId.value] == voiceId) {
            savedVoiceIds.remove(selectedModelId.value)
            viewModelScope.launch(Dispatchers.IO) { settingsRepository.clearTtsVoice(selectedModelId.value) }
        }
        mutableState.update { state ->
            state.copy(
                selectedVoiceId = state.selectedVoiceId.takeUnless { it == voiceId },
                errorMessage = if (state.selectedVoiceId == voiceId) {
                    "The selected reference was deleted. Record or import another voice."
                } else {
                    state.errorMessage
                },
            )
        }
    }

    fun microphonePermissionDenied() = mutableState.update {
        it.copy(errorMessage = "Microphone permission was denied. Allow it in Android settings and try again.")
    }

    fun previewVoice(voiceId: String) = operationController.previewVoice(voiceId)

    fun synthesize() = operationController.synthesize()

    fun prepareProfile(): Boolean {
        val snapshot = mutableState.value
        val playbackActive = snapshot.playback.status in setOf(
            SpeechPlaybackStatus.READY,
            SpeechPlaybackStatus.PLAYING,
            SpeechPlaybackStatus.PAUSED,
        )
        if (operationController.isActive() || snapshot.operation != TtsOperation.IDLE || playbackActive) return false
        val model = snapshot.selectedModel
        val voice = snapshot.selectedVoice
        val threads = snapshot.threadCount.toIntOrNull()
        val error = when {
            model?.installed != true -> "Select an installed text-to-speech model."
            voice == null -> "Select a compatible voice."
            snapshot.text.isBlank() -> "Enter text before profiling."
            threads !in 0..64 -> "Thread count must be between 0 and 64."
            else -> null
        }
        if (error != null) {
            mutableState.update { it.copy(errorMessage = error) }
            return false
        }
        val settings = runCatching {
            TtsSpeechSettingsFactory.create(snapshot, requireNotNull(voice), requireNotNull(threads))
        }.getOrElse { cause ->
            mutableState.update { it.copy(errorMessage = cause.message ?: "Text-to-speech settings are invalid.") }
            return false
        }
        profileLaunchCoordinator.open(
            BenchmarkWorkload.TextToSpeech(
                modelId = requireNotNull(model).id,
                modelDisplayName = model.displayName,
                text = snapshot.text,
                languageCode = snapshot.language.code,
                voice = settings.voiceCondition,
                speed = snapshot.speed,
                sentenceSilenceScale = snapshot.sentenceSilenceScale,
                threadCount = requireNotNull(threads),
            ),
        )
        return true
    }

    fun pausePlayback() = operationController.pausePlayback()

    fun resumePlayback() = operationController.resumePlayback()

    fun stop() = operationController.stop()

    fun replay() = operationController.replay()

    fun export(destination: Uri) {
        val output = mutableState.value.output ?: return
        Log.i(TAG, "TTS UI export started: destinationScheme=${destination.scheme}, durationMs=${output.durationMs}")
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { generatedAudioStore.export(output, destination) }
                .onSuccess {
                    Log.i(TAG, "TTS UI export completed.")
                    mutableState.update { it.copy(statusMessage = "WAV exported successfully.", errorMessage = null) }
                }
                .onFailure { error ->
                    Log.e(TAG, "TTS UI export failed: ${error.message}", error)
                    mutableState.update { it.copy(errorMessage = error.message ?: "Could not export the WAV file.") }
                }
        }
    }

    fun shareFailed(message: String) = mutableState.update { it.copy(errorMessage = message) }

    override fun onCleared() {
        operationController.clear()
        super.onCleared()
    }

    private fun refreshSystemVoices() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching(systemTextToSpeechSupport::refresh)
                .onFailure { error -> Log.w(TAG, "Android TextToSpeech voice discovery failed: ${error.message}") }
        }
    }

    private fun restoreDraft() {
        viewModelScope.launch {
            settingsRepository.ttsDraft.first()?.let { savedText ->
                if (!hasTextInput) mutableState.update { it.copy(text = savedText.take(it.characterLimit)) }
            }
        }
    }

    private fun observeAvailableVoices() {
        viewModelScope.launch {
            val selection = settingsRepository.ttsSelection.first()
            savedModelId = selection.selectedModelId?.let(::ModelId)
            savedVoiceIds.putAll(selection.voiceIdsByModel)
            combine(
                modelLibrary.installedModels,
                modelTransfers.catalog,
                referenceVoiceStore.voices,
                systemTextToSpeechSupport.voices,
            ) { installed, catalog, references, systemVoices ->
                buildList {
                    if (systemVoices.isNotEmpty()) add(androidTextToSpeechOption(systemVoices))
                    addAll(ttsModelOptions(installed, catalog))
                } to references
            }.collectLatest { (models, references) ->
                mutableState.update { current ->
                    val selected = current.selectedModelId
                        ?.takeIf { id -> models.any { it.id == id && it.installed } }
                        ?: savedModelId?.takeIf { id -> models.any { it.id == id && it.installed } }
                        ?: models.firstOrNull { it.installed }?.id
                    val selectedModel = models.firstOrNull { it.id == selected }
                    val language = if (selectedModel?.voiceMode == TtsVoiceMode.REFERENCE_AUDIO) {
                        TtsLanguage.ENGLISH
                    } else {
                        current.language
                    }
                    val voices = TtsVoiceResolver.forModel(selectedModel, language, references)
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
    }

    private fun observePlayback() {
        viewModelScope.launch {
            streamingSpeechPlayer.state.collectLatest { playback ->
                mutableState.update { it.copy(playback = playback) }
            }
        }
    }

    private fun restoreLatestOutput() {
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
    }

    private fun observeReplayRequests() {
        viewModelScope.launch {
            replayStore.pending.collectLatest { run ->
                if (run?.capability == AiCapability.TEXT_TO_SPEECH) applyReplay(run)
            }
        }
    }

    private fun applyReplay(run: RunRecord) {
        val restored = TtsReplayRestorer.restore(mutableState.value, run)
        mutableState.value = restored.state
        restored.selectedModelIdToPersist?.let { selectedModelId ->
            savedModelId = selectedModelId
            persistModelSelection(selectedModelId)
            mutableState.value.selectedVoice?.let(::persistVoiceSelection)
        }
        replayStore.consume(run.id)
    }

    private fun persistModelSelection(modelId: ModelId) {
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.updateTtsSelectedModel(modelId.value) }
    }

    private fun persistVoiceSelection(voice: TtsVoiceOption) {
        val modelId = mutableState.value.selectedModelId ?: return
        savedVoiceIds[modelId.value] = voice.id
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.updateTtsVoice(modelId.value, voice.id) }
    }

    private companion object {
        const val TAG = "AiP123Tts"
    }
}
