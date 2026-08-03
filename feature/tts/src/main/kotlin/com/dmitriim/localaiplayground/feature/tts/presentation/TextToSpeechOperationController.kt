package com.dmitriim.localaiplayground.feature.tts.presentation

import android.net.Uri
import android.util.Log
import com.dmitriim.localaiplayground.core.audio.input.storage.ReferenceVoiceStore
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackStatus
import com.dmitriim.localaiplayground.core.model.runs.RunStatus
import com.dmitriim.localaiplayground.core.result.ForegroundOperationCoordinator
import com.dmitriim.localaiplayground.feature.tts.domain.PersistTtsRun
import com.dmitriim.localaiplayground.feature.tts.domain.PreviewSpeech
import com.dmitriim.localaiplayground.feature.tts.domain.SpeechPreviewRequest
import com.dmitriim.localaiplayground.feature.tts.domain.SpeechSynthesisEvent
import com.dmitriim.localaiplayground.feature.tts.domain.SpeechSynthesisRequest
import com.dmitriim.localaiplayground.feature.tts.domain.SynthesizeSpeech
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
                statusMessage = "Record 5–10 seconds of clear speech. Recording stops automatically at 10 seconds.",
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
                        statusMessage = "Saved reference voice “${voice.displayName}”.",
                    )
                }
                state.value.selectedVoice?.let(onVoiceSelected)
            } catch (_: CancellationException) {
                state.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        referenceLevel = null,
                        statusMessage = "Reference recording stopped.",
                    )
                }
            } catch (error: Throwable) {
                state.update {
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
                statusMessage = "Normalizing reference audio to mono 24 kHz…",
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
                        statusMessage = "Saved reference voice “${voice.displayName}”.",
                    )
                }
                state.value.selectedVoice?.let(onVoiceSelected)
            } catch (_: CancellationException) {
                state.update { it.copy(operation = TtsOperation.IDLE) }
            } catch (error: Throwable) {
                state.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        errorMessage = error.message ?: "Could not import the reference audio.",
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
            state.update { it.copy(errorMessage = "That voice does not support ${snapshot.language.label}.") }
            return
        }
        if (snapshot.text.isBlank()) {
            state.update { it.copy(errorMessage = "Enter text before previewing a voice.") }
            return
        }
        val threads = snapshot.threadCount.toIntOrNull() ?: run {
            state.update { it.copy(errorMessage = "Thread count must be a whole number.") }
            return
        }
        if (activeJob != null) {
            state.update {
                it.copy(
                    operation = TtsOperation.CANCELLING,
                    previewVoiceId = voice.id,
                    statusMessage = "Switching preview to ${voice.displayName}…",
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
                    statusMessage = "Previewing ${voice.displayName} locally…",
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
                        statusMessage = "Voice preview completed.",
                    )
                }
            } catch (_: CancellationException) {
                state.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        previewVoiceId = null,
                        statusMessage = "Voice preview stopped.",
                    )
                }
            } catch (error: Throwable) {
                Log.e(TAG, "TTS voice preview failed: ${error.message}", error)
                state.update {
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
        val snapshot = state.value
        val modelId = snapshot.selectedModelId ?: run {
            state.update { it.copy(errorMessage = "Install a compatible voice model before synthesizing speech.") }
            return
        }
        val voice = snapshot.selectedVoice ?: run {
            state.update { it.copy(errorMessage = "Select a voice that supports ${snapshot.language.label}.") }
            return
        }
        val threads = snapshot.threadCount.toIntOrNull() ?: run {
            state.update { it.copy(errorMessage = "Thread count must be a whole number.") }
            return
        }
        val startedAt = System.currentTimeMillis()
        val model = snapshot.selectedModel
        Log.i(TAG, "TTS UI synthesis started: model=${model?.displayName}, textLength=${snapshot.text.length}, language=${snapshot.language.code}, voice=${voice.id}, speaker=${voice.speakerId}, speed=${snapshot.speed}, silenceScale=${snapshot.sentenceSilenceScale}, volume=${snapshot.volume}, threads=$threads, audioEffects=${snapshot.audioEffects}")
        state.update {
            it.copy(
                operation = TtsOperation.SYNTHESIZING,
                previewVoiceId = null,
                metrics = null,
                errorMessage = null,
                statusMessage = when {
                    snapshot.usesReferenceVoice -> "Generating Chatterbox speech locally…"
                    snapshot.usesPlatformVoice -> "Synthesizing with Android’s on-device voice…"
                    else -> "Synthesizing and streaming PCM locally…"
                },
            )
        }
        operationJob = scope.launch(Dispatchers.Default) {
            try {
                synthesizeSpeech.execute(
                    SpeechSynthesisRequest(
                        modelId = modelId,
                        text = snapshot.text,
                        settings = TtsSpeechSettingsFactory.create(snapshot, voice, threads),
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
                                    statusMessage = "Synthesis completed; playback is draining by presented frames.",
                                )
                            }
                        }
                        is SpeechSynthesisEvent.Completed -> state.update {
                            it.copy(
                                operation = TtsOperation.IDLE,
                                output = event.output,
                                metrics = event.metrics,
                                statusMessage = "Latest WAV retained in app-private storage until the next successful synthesis.",
                            )
                        }.also {
                            Log.i(TAG, "TTS UI received completed event: synthesisMs=${event.metrics.synthesisDurationMs}, underruns=${event.metrics.playbackUnderrunCount}")
                            persistTtsRun(TtsRunSnapshotFactory.create(RunStatus.SUCCEEDED, startedAt, model, snapshot, event.metrics, null))
                        }
                    }
                }
            } catch (_: CancellationException) {
                Log.i(TAG, "TTS UI operation cancelled.")
                state.update { it.copy(operation = TtsOperation.IDLE, statusMessage = "Speech operation stopped.") }
                persistTtsRun(TtsRunSnapshotFactory.create(RunStatus.CANCELLED, startedAt, model, snapshot, null, "Speech operation stopped."))
            } catch (error: Throwable) {
                Log.e(TAG, "TTS UI operation failed: ${error.message}", error)
                val message = error.message ?: "Local speech synthesis failed."
                state.update {
                    it.copy(
                        operation = TtsOperation.IDLE,
                        errorMessage = message,
                        statusMessage = null,
                    )
                }
                persistTtsRun(TtsRunSnapshotFactory.create(RunStatus.FAILED, startedAt, model, snapshot, null, message))
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
        state.update { it.copy(errorMessage = null, statusMessage = "Replaying the retained WAV.") }
        operationJob = scope.launch(Dispatchers.Default) {
            try {
                synthesizeSpeech.replay(output, volume)
                Log.i(TAG, "TTS UI replay completed.")
                state.update { it.copy(statusMessage = "Replay completed.") }
            } catch (_: CancellationException) {
                Log.i(TAG, "TTS UI replay cancelled.")
                state.update { it.copy(statusMessage = "Playback stopped.") }
            } catch (error: Throwable) {
                Log.e(TAG, "TTS UI replay failed: ${error.message}", error)
                state.update { it.copy(errorMessage = error.message ?: "Could not replay generated speech.") }
            }
        }.also(::registerForegroundCancellation)
    }

    fun clear() {
        Log.i(TAG, "TTS ViewModel cleared; cancelling active work.")
        previewSpeech.cancel()
        synthesizeSpeech.cancel()
        referenceVoiceStore.stopCapture()
        operationJob?.cancel()
        synthesizeSpeech.unloadRuntime()
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
