package com.dmitriim.localailab.feature.assistant.impl.presentation

import com.dmitriim.localailab.core.audio.input.model.PcmAudioInput
import com.dmitriim.localailab.feature.assistant.impl.domain.AssistantRunRecorder
import com.dmitriim.localailab.feature.assistant.impl.domain.stt.AssistantAudioRecorder
import com.dmitriim.localailab.feature.assistant.impl.domain.stt.AssistantTranscriber
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.AssistantInputMode
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.AssistantOperation
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.AssistantUiState
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.SpeechModelOption
import com.dmitriim.localailab.feature.runs.api.domain.history.RunModelSnapshot
import com.dmitriim.localailab.feature.runs.api.domain.history.RunStatus
import com.dmitriim.localailab.feature.stt.api.domain.SpeechTranscriptionEvent
import com.dmitriim.localailab.feature.stt.api.domain.SpeechTranscriptionMetrics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

internal class AssistantSttController(
    private val host: AssistantOperationHost,
    private val audioRecorder: AssistantAudioRecorder,
    private val transcriber: AssistantTranscriber,
    private val runRecorder: AssistantRunRecorder,
    private val chatController: AssistantChatController,
) {
    fun startRecording() {
        val snapshot = host.state.value
        if (!snapshot.isIdle) return
        val speechModel = snapshot.selectedSpeechModel?.takeIf { it.installed }
        if (speechModel == null) {
            host.state.update {
                it.copy(errorMessage = "Configure speech-to-text before using the microphone.")
            }
            return
        }
        if (snapshot.inputMode == AssistantInputMode.VOICE) {
            if (snapshot.voiceConfigurationError != null) return
        }
        val settingsError = runCatching(snapshot.speechInputSettings::validate).exceptionOrNull()?.message
        if (settingsError != null) {
            host.state.update { it.copy(errorMessage = settingsError) }
            return
        }
        host.launchForeground { recordAndTranscribe(snapshot, speechModel) }
    }

    fun stopRecording() {
        if (host.state.value.operation != AssistantOperation.Recording) return
        host.state.update {
            it.copy(
                operation = AssistantOperation.Transcribing,
                level = null,
                statusMessage = "Finalizing recorded speech…",
            )
        }
        audioRecorder.stop()
    }

    suspend fun recordAndTranscribe(initial: AssistantUiState, speechModel: SpeechModelOption) {
        val startedAt = System.currentTimeMillis()
        var input: PcmAudioInput? = null
        var transcript: String? = null
        var metrics: SpeechTranscriptionMetrics? = null
        host.activeLinkedRunIds.clear()
        try {
            host.state.update {
                it.copy(
                    operation = AssistantOperation.Recording,
                    level = null,
                    statusMessage = "Listening…",
                    errorMessage = null,
                )
            }
            input = audioRecorder.record(speechModel.sampleRateHz) { level ->
                host.state.update { it.copy(level = level) }
            }
            host.state.update {
                it.copy(
                    operation = AssistantOperation.Transcribing,
                    level = null,
                    statusMessage = "Transcribing locally…",
                )
            }
            transcriber.transcribe(
                modelId = speechModel.id,
                input = input,
                languageCode = initial.speechInputSettings.languageCode,
                threadCount = initial.speechInputSettings.threadCount,
            ).collect { event ->
                if (event is SpeechTranscriptionEvent.Completed) {
                    transcript = event.transcript.trim()
                    metrics = event.metrics
                }
            }
            require(!transcript.isNullOrBlank()) { "No speech was recognized. Record another turn and try again." }
            host.activeLinkedRunIds += runRecorder.recordSpeechInput(
                status = RunStatus.SUCCEEDED,
                startedAtEpochMs = startedAt,
                model = speechModel.toRunSnapshot(),
                transcript = transcript,
                languageCode = initial.speechInputSettings.languageCode,
                threadCount = initial.speechInputSettings.threadCount.toInt(),
                metrics = metrics,
                error = null,
            )
            if (initial.inputMode == AssistantInputMode.DICTATE) {
                host.state.update {
                    it.copy(
                        input = appendTranscript(initial.input, requireNotNull(transcript)),
                        operation = AssistantOperation.Idle,
                        statusMessage = "Transcript added to the message draft.",
                    )
                }
            } else {
                val response = chatController.generateFromVoice(initial, requireNotNull(transcript))
                runRecorder.recordVoiceTurn(
                    status = if (response.speechSucceeded) RunStatus.SUCCEEDED else RunStatus.FAILED,
                    startedAtEpochMs = startedAt,
                    transcript = transcript,
                    response = response.text,
                    linkedRunIds = host.activeLinkedRunIds.toList(),
                    error = response.speechError,
                )
            }
        } catch (_: CancellationException) {
            withContext(NonCancellable) {
                recordVoiceInputFailure(
                    status = RunStatus.CANCELLED,
                    startedAt = startedAt,
                    speechModel = speechModel,
                    transcript = transcript,
                    initial = initial,
                    metrics = metrics,
                    inputError = "Voice input cancelled.",
                    turnError = "Voice turn cancelled.",
                )
            }
            host.state.update {
                it.copy(
                    operation = AssistantOperation.Idle,
                    level = null,
                    speakingMessageId = null,
                    statusMessage = "Voice operation stopped.",
                )
            }
        } catch (error: Throwable) {
            withContext(NonCancellable) {
                recordVoiceInputFailure(
                    status = RunStatus.FAILED,
                    startedAt = startedAt,
                    speechModel = speechModel,
                    transcript = transcript,
                    initial = initial,
                    metrics = metrics,
                    inputError = error.message,
                    turnError = error.message,
                )
            }
            host.handleOperationFailure(error)
        } finally {
            audioRecorder.clear(input)
        }
    }

    fun cancel() {
        audioRecorder.stop()
        transcriber.cancel()
    }

    private suspend fun recordVoiceInputFailure(
        status: RunStatus,
        startedAt: Long,
        speechModel: SpeechModelOption,
        transcript: String?,
        initial: AssistantUiState,
        metrics: SpeechTranscriptionMetrics?,
        inputError: String?,
        turnError: String?,
    ) {
        if (host.activeLinkedRunIds.isEmpty()) {
            host.activeLinkedRunIds += runRecorder.recordSpeechInput(
                status,
                startedAt,
                speechModel.toRunSnapshot(),
                transcript,
                initial.speechInputSettings.languageCode,
                initial.speechInputSettings.threadCount.toIntOrNull() ?: 0,
                metrics,
                inputError,
            )
        }
        if (initial.inputMode == AssistantInputMode.VOICE) {
            runRecorder.recordVoiceTurn(
                status = status,
                startedAtEpochMs = startedAt,
                transcript = transcript,
                response = null,
                linkedRunIds = host.activeLinkedRunIds.toList(),
                error = turnError,
            )
        }
    }

    private fun SpeechModelOption.toRunSnapshot() = RunModelSnapshot(id.value, displayName, engineId.value)

    private fun appendTranscript(existing: String, transcript: String): String = when {
        existing.isBlank() -> transcript
        existing.last().isWhitespace() -> existing + transcript
        else -> "$existing $transcript"
    }
}
