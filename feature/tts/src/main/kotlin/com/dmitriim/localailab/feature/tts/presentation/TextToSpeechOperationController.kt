package com.dmitriim.localailab.feature.tts.presentation

import android.net.Uri
import android.util.Log
import com.dmitriim.localailab.core.audio.input.storage.ReferenceVoiceStore
import com.dmitriim.localailab.core.audio.output.model.SpeechPlaybackStatus
import com.dmitriim.localailab.core.model.runs.RunStatus
import com.dmitriim.localailab.core.result.ForegroundOperationCoordinator
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.text.UiText
import com.dmitriim.localailab.core.voice.tts.PreviewSpeech
import com.dmitriim.localailab.core.voice.tts.SpeechPreviewRequest
import com.dmitriim.localailab.core.voice.tts.SpeechSynthesisEvent
import com.dmitriim.localailab.core.voice.tts.SpeechSynthesisRequest
import com.dmitriim.localailab.core.voice.tts.SynthesizeSpeech
import com.dmitriim.localailab.feature.tts.domain.PersistTtsRun
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Owns the single foreground TTS operation and translates its outcomes into screen state. */
internal class TextToSpeechOperationController(
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<TextToSpeechUiState>,
    private val synthesizeSpeech: SynthesizeSpeech,
    private val previewSpeech: PreviewSpeech,
    private val referenceVoiceStore: ReferenceVoiceStore,
    private val operationCoordinator: ForegroundOperationCoordinator,
    private val persistTtsRun: PersistTtsRun,
    private val onVoiceSelected: (TtsVoiceOption) -> Unit,
) {
    private var operationJob: Job? = null

    fun isActive(): Boolean = operationJob?.isActive == true

    fun unloadRuntime() {
        synthesizeSpeech.unloadRuntime()
    }

    fun startReferenceRecording() {
        if (isActive() || !state.value.usesReferenceVoice) return
        state.update {
            it.copy(
                operation = TtsOperation.RECORDING_REFERENCE,
                referenceLevel = null,
                errorMessage = null,
                statusMessage = UiText.Resource(CoreUiR.string.tts_status_record_reference),
            )
        }
        operationJob = scope.launch(Dispatchers.Default) {
            try {
                val voice = referenceVoiceStore.capture { level ->
                    state.update { it.copy(referenceLevel = level) }
                }
                state.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        referenceLevel = null,
                        referenceVoices = referenceVoiceStore.voices.value,
                        selectedVoiceId = voice.id,
                        statusMessage = UiText.Resource(CoreUiR.string.tts_status_saved_reference, listOf(voice.displayName)),
                    )
                }
                state.value.selectedVoice?.let(onVoiceSelected)
            } catch (_: CancellationException) {
                state.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        referenceLevel = null,
                        statusMessage = UiText.Resource(CoreUiR.string.tts_status_reference_recording_stopped),
                    )
                }
            } catch (error: Throwable) {
                state.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        referenceVoices = referenceVoiceStore.voices.value,
                        referenceLevel = null,
                        errorMessage = error.message?.let(UiText::Dynamic) ?: UiText.Resource(CoreUiR.string.tts_error_record_reference),
                        statusMessage = null,
                    )
                }
            }
        }.also(::registerForegroundCancellation)
    }

    fun stopReferenceRecording() {
        if (state.value.operation != TtsOperation.RECORDING_REFERENCE) return
        state.update { it.copy(operation = TtsOperation.STOPPING_REFERENCE) }
        referenceVoiceStore.stopCapture()
    }

    fun importReferenceAudio(uri: Uri) {
        if (isActive() || !state.value.usesReferenceVoice) return
        state.update {
            it.copy(
                operation = TtsOperation.IMPORTING_REFERENCE,
                errorMessage = null,
                statusMessage = UiText.Resource(CoreUiR.string.tts_status_normalizing_reference),
            )
        }
        operationJob = scope.launch(Dispatchers.Default) {
            try {
                val voice = referenceVoiceStore.importAudio(uri)
                state.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        referenceVoices = referenceVoiceStore.voices.value,
                        selectedVoiceId = voice.id,
                        statusMessage = UiText.Resource(CoreUiR.string.tts_status_saved_reference, listOf(voice.displayName)),
                    )
                }
                state.value.selectedVoice?.let(onVoiceSelected)
            } catch (_: CancellationException) {
                state.update { it.copy(operation = TtsOperation.IDLE) }
            } catch (error: Throwable) {
                state.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        errorMessage = error.message?.let(UiText::Dynamic) ?: UiText.Resource(CoreUiR.string.tts_error_import_reference),
                        statusMessage = null,
                    )
                }
            }
        }.also(::registerForegroundCancellation)
    }

    fun previewVoice(voiceId: String) {
        val snapshot = state.value
        val activeJob = operationJob?.takeIf(Job::isActive)
        if (activeJob != null && snapshot.operation != TtsOperation.PREVIEWING) return
        if (snapshot.operation == TtsOperation.PREVIEWING && snapshot.previewVoiceId == voiceId) {
            stop()
            return
        }
        val modelId = snapshot.selectedModelId ?: return
        val voice = snapshot.compatibleVoices.firstOrNull { it.id == voiceId } ?: run {
            state.update { it.copy(errorMessage = UiText.Resource(CoreUiR.string.tts_error_voice_unsupported, listOf(snapshot.language.label))) }
            return
        }
        if (snapshot.text.isBlank()) {
            state.update { it.copy(errorMessage = UiText.Resource(CoreUiR.string.tts_error_enter_text_preview)) }
            return
        }
        val threads = snapshot.threadCount.toIntOrNull() ?: run {
            state.update { it.copy(errorMessage = UiText.Resource(CoreUiR.string.tts_error_thread_count_integer)) }
            return
        }
        if (activeJob != null) {
            state.update {
                it.copy(
                    operation = TtsOperation.CANCELLING,
                    previewVoiceId = voice.id,
                    statusMessage = UiText.Resource(CoreUiR.string.tts_status_switching_preview, listOf(voice.displayName)),
                )
            }
            previewSpeech.cancel()
            activeJob.cancel()
        }
        operationJob = scope.launch(Dispatchers.Default) {
            activeJob?.join()
            state.update {
                it.copy(
                    operation = TtsOperation.PREVIEWING,
                    previewVoiceId = voice.id,
                    errorMessage = null,
                    statusMessage = UiText.Resource(CoreUiR.string.tts_status_previewing_voice, listOf(voice.displayName)),
                )
            }
            try {
                previewSpeech.execute(
                    SpeechPreviewRequest(
                        modelId = modelId,
                        text = snapshot.text,
                        voiceName = voice.displayName,
                        settings = TtsSpeechSettingsFactory.create(snapshot, voice, threads),
                    ),
                )
                state.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        previewVoiceId = null,
                        statusMessage = UiText.Resource(CoreUiR.string.tts_status_preview_completed),
                    )
                }
            } catch (_: CancellationException) {
                state.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        previewVoiceId = null,
                        statusMessage = UiText.Resource(CoreUiR.string.tts_status_preview_stopped),
                    )
                }
            } catch (error: Throwable) {
                Log.e(TAG, "TTS voice preview failed: ${error.message}", error)
                state.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        previewVoiceId = null,
                        errorMessage = error.message?.let(UiText::Dynamic) ?: UiText.Resource(CoreUiR.string.tts_error_preview_voice),
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
        val snapshot = state.value
        val modelId = snapshot.selectedModelId ?: run {
            state.update { it.copy(errorMessage = UiText.Resource(CoreUiR.string.tts_error_install_model)) }
            return
        }
        val voice = snapshot.selectedVoice ?: run {
            state.update { it.copy(errorMessage = UiText.Resource(CoreUiR.string.tts_error_select_voice, listOf(snapshot.language.label))) }
            return
        }
        val threads = snapshot.threadCount.toIntOrNull() ?: run {
            state.update { it.copy(errorMessage = UiText.Resource(CoreUiR.string.tts_error_thread_count_integer)) }
            return
        }
        val startedAt = System.currentTimeMillis()
        val runId = UUID.randomUUID().toString()
        val model = snapshot.selectedModel
        Log.i(TAG, "TTS UI synthesis started: model=${model?.displayName}, textLength=${snapshot.text.length}, language=${snapshot.language.code}, voice=${voice.id}, speaker=${voice.speakerId}, speed=${snapshot.speed}, silenceScale=${snapshot.sentenceSilenceScale}, volume=${snapshot.volume}, threads=$threads, audioEffects=${snapshot.audioEffects}")
        beginSynthesis(snapshot)
        operationJob = scope.launch(Dispatchers.Default) {
            try {
                synthesizeSpeech.execute(
                    SpeechSynthesisRequest(
                        modelId = modelId,
                        text = snapshot.text,
                        settings = TtsSpeechSettingsFactory.create(snapshot, voice, threads),
                        runId = runId,
                    ),
                ).collect { event ->
                    when (event) {
                        is SpeechSynthesisEvent.Prepared -> Log.i(TAG, "TTS UI received prepared event: loadMs=${event.loadDurationMs}, sampleRateHz=${event.sampleRateHz}, speakers=${event.speakerCount}")
                        is SpeechSynthesisEvent.Synthesized -> {
                            Log.i(TAG, "TTS UI received synthesized event: durationMs=${event.synthesisDurationMs}, outputDurationMs=${event.output.durationMs}")
                            state.update {
                                it.copy(
                                    operation = TtsOperation.IDLE,
                                    output = event.output,
                                    statusMessage = UiText.Resource(CoreUiR.string.tts_status_synthesis_draining),
                                )
                            }
                        }
                        is SpeechSynthesisEvent.Completed -> state.update {
                            it.copy(
                                operation = TtsOperation.IDLE,
                                output = event.output,
                                metrics = event.metrics,
                                statusMessage = UiText.Resource(CoreUiR.string.tts_status_wav_retained),
                            )
                        }.also {
                            Log.i(TAG, "TTS UI received completed event: synthesisMs=${event.metrics.synthesisDurationMs}, underruns=${event.metrics.playbackUnderrunCount}")
                            persistTtsRun(TtsRunSnapshotFactory.create(runId, RunStatus.SUCCEEDED, startedAt, model, snapshot, event.metrics, null))
                        }
                    }
                }
            } catch (_: CancellationException) {
                Log.i(TAG, "TTS UI operation cancelled.")
                state.update { it.copy(operation = TtsOperation.IDLE, statusMessage = UiText.Resource(CoreUiR.string.tts_status_operation_stopped)) }
                persistTtsRun(TtsRunSnapshotFactory.create(runId, RunStatus.CANCELLED, startedAt, model, snapshot, null, "Speech operation stopped."))
            } catch (error: Throwable) {
                Log.e(TAG, "TTS UI operation failed: ${error.message}", error)
                val message = error.message ?: "Local speech synthesis failed."
                state.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        errorMessage = UiText.Dynamic(message),
                        statusMessage = null,
                    )
                }
                persistTtsRun(TtsRunSnapshotFactory.create(runId, RunStatus.FAILED, startedAt, model, snapshot, null, message))
            }
        }.also(::registerForegroundCancellation)
    }

    fun pausePlayback() {
        Log.i(TAG, "TTS UI pause requested.")
        synthesizeSpeech.pausePlayback()
    }

    private fun beginSynthesis(snapshot: TextToSpeechUiState) {
        state.update {
            it.copy(
                operation = TtsOperation.SYNTHESIZING,
                previewVoiceId = null,
                metrics = null,
                errorMessage = null,
                statusMessage = when {
                    snapshot.usesReferenceVoice -> UiText.Resource(CoreUiR.string.tts_status_generating_chatterbox)
                    snapshot.usesPlatformVoice -> UiText.Resource(CoreUiR.string.tts_status_synthesizing_platform)
                    else -> UiText.Resource(CoreUiR.string.tts_status_synthesizing_pcm)
                },
            )
        }
    }

    fun resumePlayback() {
        Log.i(TAG, "TTS UI resume requested.")
        synthesizeSpeech.resumePlayback()
    }

    fun stop() {
        if (!isActive() && state.value.playback.status !in activePlaybackStatuses) return
        Log.i(TAG, "TTS UI stop requested: operation=${state.value.operation}, playback=${state.value.playback.status}")
        state.update { it.copy(operation = TtsOperation.CANCELLING) }
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
        val output = state.value.output ?: return
        val volume = state.value.volume
        Log.i(TAG, "TTS UI replay started: durationMs=${output.durationMs}, volume=$volume")
        state.update { it.copy(errorMessage = null, statusMessage = UiText.Resource(CoreUiR.string.tts_status_replaying_wav)) }
        operationJob = scope.launch(Dispatchers.Default) {
            try {
                synthesizeSpeech.replay(output, volume)
                Log.i(TAG, "TTS UI replay completed.")
                state.update { it.copy(statusMessage = UiText.Resource(CoreUiR.string.tts_status_replay_completed)) }
            } catch (_: CancellationException) {
                Log.i(TAG, "TTS UI replay cancelled.")
                state.update { it.copy(statusMessage = UiText.Resource(CoreUiR.string.tts_status_playback_stopped)) }
            } catch (error: Throwable) {
                Log.e(TAG, "TTS UI replay failed: ${error.message}", error)
                state.update { it.copy(errorMessage = error.message?.let(UiText::Dynamic) ?: UiText.Resource(CoreUiR.string.tts_error_replay)) }
            }
        }.also(::registerForegroundCancellation)
    }

    fun clear() {
        Log.i(TAG, "TTS ViewModel cleared; cancelling active work.")
        previewSpeech.cancel()
        synthesizeSpeech.cancel()
        referenceVoiceStore.stopCapture()
        operationJob?.cancel()
    }

    private fun registerForegroundCancellation(job: Job) {
        val registration = operationCoordinator.register(::stop)
        job.invokeOnCompletion { registration.close() }
    }

    private companion object {
        const val TAG = "AiP123Tts"
        val activePlaybackStatuses = setOf(
            SpeechPlaybackStatus.READY,
            SpeechPlaybackStatus.PLAYING,
            SpeechPlaybackStatus.PAUSED,
        )
    }
}
