package com.dmitriim.localaiplayground.feature.assistant.presentation

import android.util.Log
import com.dmitriim.localaiplayground.ai.api.llm.ChatEngine
import com.dmitriim.localaiplayground.core.audio.input.model.PcmAudioInput
import com.dmitriim.localaiplayground.core.model.runs.RunModelSnapshot
import com.dmitriim.localaiplayground.core.model.runs.RunStatus
import com.dmitriim.localaiplayground.core.result.ForegroundOperationCoordinator
import com.dmitriim.localaiplayground.core.voice.stt.SpeechTranscriptionEvent
import com.dmitriim.localaiplayground.core.voice.stt.SpeechTranscriptionMetrics
import com.dmitriim.localaiplayground.core.voice.tts.SpeechSynthesisEvent
import com.dmitriim.localaiplayground.core.voice.tts.SpeechSynthesisMetrics
import com.dmitriim.localaiplayground.feature.assistant.domain.AssistantAudioRecorder
import com.dmitriim.localaiplayground.feature.assistant.domain.AssistantRunRecorder
import com.dmitriim.localaiplayground.feature.assistant.domain.AssistantSpeechOutput
import com.dmitriim.localaiplayground.feature.assistant.domain.AssistantTranscriber
import com.dmitriim.localaiplayground.feature.assistant.domain.ChatGenerationEvent
import com.dmitriim.localaiplayground.feature.assistant.domain.ChatGenerationRequest
import com.dmitriim.localaiplayground.feature.assistant.domain.GenerateAssistantResponse
import com.dmitriim.localaiplayground.feature.assistant.domain.PersistAssistantTurn
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns the one cancellable assistant workflow and maps its progress to screen state. */
internal class AssistantOperationController(
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<AssistantUiState>,
    private val chatEngine: ChatEngine,
    private val generateResponse: GenerateAssistantResponse,
    private val audioRecorder: AssistantAudioRecorder,
    private val transcriber: AssistantTranscriber,
    private val speechOutput: AssistantSpeechOutput,
    private val runRecorder: AssistantRunRecorder,
    private val persistAssistantTurn: PersistAssistantTurn,
    private val operationCoordinator: ForegroundOperationCoordinator,
    private val conversationId: () -> String,
) {
    private var activeJob: Job? = null
    private val activeLinkedRunIds = mutableListOf<String>()

    fun unloadChatRuntime() {
        if (!state.value.isIdle) return
        scope.launch(Dispatchers.Default) {
            runCatching { chatEngine.unload() }
                .onSuccess { state.update { it.copy(statusMessage = "Chat model unloaded.") } }
                .onFailure { error ->
                    state.update { it.copy(errorMessage = error.message ?: "Could not unload the chat model.") }
                }
        }
    }

    fun unloadSpeechOutputRuntime() {
        scope.launch(Dispatchers.Default) { speechOutput.unload() }
    }

    fun send() {
        val snapshot = state.value
        val input = snapshot.input.trim()
        if (input.isEmpty() || !snapshot.isIdle) return
        launchForeground {
            runCatching {
                generateInternal(
                    base = snapshot.messages,
                    userText = input,
                    appendUser = true,
                    speakAfter = snapshot.inputMode == AssistantInputMode.VOICE,
                )
            }.onFailure(::handleOperationFailure)
        }
    }

    fun startRecording() {
        val snapshot = state.value
        if (!snapshot.isIdle) return
        val speechModel = snapshot.selectedSpeechModel?.takeIf { it.installed }
        if (speechModel == null) {
            state.update { it.copy(errorMessage = "Configure speech-to-text before using the microphone.") }
            return
        }
        if (snapshot.inputMode == AssistantInputMode.VOICE) {
            if (snapshot.voiceConfigurationError != null) return
        }
        val settingsError = runCatching(snapshot.speechInputSettings::validate).exceptionOrNull()?.message
        if (settingsError != null) {
            state.update { it.copy(errorMessage = settingsError) }
            return
        }
        launchForeground { recordAndTranscribe(snapshot, speechModel) }
    }

    fun stopRecording() {
        if (state.value.operation != AssistantOperation.Recording) return
        state.update {
            it.copy(operation = AssistantOperation.Transcribing, level = null, statusMessage = "Finalizing recorded speech…")
        }
        audioRecorder.stop()
    }

    fun speakMessage(messageId: String) {
        val snapshot = state.value
        if (!snapshot.isIdle) return
        val message = snapshot.messages.firstOrNull {
            it.id == messageId && it.role == ChatMessageRole.ASSISTANT && !it.streaming && it.content.isNotBlank()
        } ?: return
        launchForeground {
            val outcome = speakInternal(message.content, message.id)
            if (!outcome.succeeded && outcome.error != null) state.update { it.copy(errorMessage = outcome.error) }
        }
    }

    fun previewVoice(modelId: com.dmitriim.localaiplayground.core.model.manifest.ModelId, voiceId: String, settings: SpeechOutputSettings): String? {
        val snapshot = state.value
        if (!snapshot.isIdle) return "Wait for the current operation to finish."
        val model = snapshot.voiceModels.firstOrNull { it.id == modelId && it.installed }
            ?: return "Select an installed text-to-speech model."
        val validationError = runCatching(settings::validate).exceptionOrNull()?.message
        if (validationError != null) return validationError
        val voice = model.compatibleVoices(settings.languageCode).firstOrNull { it.id == voiceId }
            ?: return "Select a voice compatible with this language."
        launchForeground {
            try {
                state.update {
                    it.copy(
                        operation = AssistantOperation.Speaking,
                        statusMessage = "Previewing ${voice.displayName}…",
                        errorMessage = null,
                    )
                }
                speechOutput.preview(model.id, model, voice, snapshot.speechOutputSettings)
                state.update { it.copy(operation = AssistantOperation.Idle, statusMessage = "Voice preview completed.") }
            } catch (_: CancellationException) {
                state.update { it.copy(operation = AssistantOperation.Idle, statusMessage = "Voice preview stopped.") }
            } catch (error: Throwable) {
                state.update {
                    it.copy(operation = AssistantOperation.Idle, errorMessage = error.message ?: "Could not preview this voice.")
                }
            }
        }
        return null
    }

    fun regenerate() {
        val snapshot = state.value
        val assistantIndex = snapshot.messages.indexOfLast { it.role == ChatMessageRole.ASSISTANT }
        if (assistantIndex < 0 || !snapshot.isIdle) return
        val user = snapshot.messages.subList(0, assistantIndex).lastOrNull { it.role == ChatMessageRole.USER } ?: return
        launchForeground {
            runCatching {
                generateInternal(
                    base = snapshot.messages.take(assistantIndex),
                    userText = user.content,
                    appendUser = false,
                    speakAfter = snapshot.inputMode == AssistantInputMode.VOICE,
                )
            }.onFailure(::handleOperationFailure)
        }
    }

    fun cancel() {
        if (activeJob?.isActive != true) return
        state.update { it.copy(operation = AssistantOperation.Cancelling, statusMessage = "Stopping…") }
        audioRecorder.stop()
        transcriber.cancel()
        chatEngine.cancel()
        speechOutput.cancel()
        activeJob?.cancel(CancellationException("Assistant operation cancelled"))
    }

    fun clear() {
        audioRecorder.stop()
        transcriber.cancel()
        chatEngine.cancel()
        speechOutput.cancel()
        activeJob?.cancel()
    }

    private suspend fun recordAndTranscribe(initial: AssistantUiState, speechModel: SpeechModelOption) {
        val startedAt = System.currentTimeMillis()
        var input: PcmAudioInput? = null
        var transcript: String? = null
        var metrics: SpeechTranscriptionMetrics? = null
        activeLinkedRunIds.clear()
        try {
            state.update {
                it.copy(operation = AssistantOperation.Recording, level = null, statusMessage = "Listening…", errorMessage = null)
            }
            input = audioRecorder.record(speechModel.sampleRateHz) { level -> state.update { it.copy(level = level) } }
            state.update {
                it.copy(operation = AssistantOperation.Transcribing, level = null, statusMessage = "Transcribing locally…")
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
            activeLinkedRunIds += runRecorder.recordSpeechInput(
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
                state.update {
                    it.copy(
                        input = appendTranscript(initial.input, requireNotNull(transcript)),
                        operation = AssistantOperation.Idle,
                        statusMessage = "Transcript added to the message draft.",
                    )
                }
            } else {
                val response = generateInternal(initial.messages, requireNotNull(transcript), appendUser = true, speakAfter = true)
                runRecorder.recordVoiceTurn(
                    status = if (response.speechSucceeded) RunStatus.SUCCEEDED else RunStatus.FAILED,
                    startedAtEpochMs = startedAt,
                    transcript = transcript,
                    response = response.text,
                    linkedRunIds = activeLinkedRunIds.toList(),
                    error = response.speechError,
                )
            }
        } catch (_: CancellationException) {
            withContext(NonCancellable) { recordVoiceInputFailure(RunStatus.CANCELLED, startedAt, speechModel, transcript, initial, metrics, "Voice input cancelled.", "Voice turn cancelled.") }
            state.update {
                it.copy(operation = AssistantOperation.Idle, level = null, speakingMessageId = null, statusMessage = "Voice operation stopped.")
            }
        } catch (error: Throwable) {
            withContext(NonCancellable) { recordVoiceInputFailure(RunStatus.FAILED, startedAt, speechModel, transcript, initial, metrics, error.message, error.message) }
            handleOperationFailure(error)
        } finally {
            audioRecorder.clear(input)
        }
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
        if (activeLinkedRunIds.isEmpty()) {
            activeLinkedRunIds += runRecorder.recordSpeechInput(
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
            runRecorder.recordVoiceTurn(status, startedAt, transcript, null, activeLinkedRunIds.toList(), turnError)
        }
    }

    private suspend fun generateInternal(base: List<ChatMessage>, userText: String, appendUser: Boolean, speakAfter: Boolean): GenerationOutcome {
        val snapshot = state.value
        val selectedId = snapshot.selectedChatModelId ?: error("Select an installed chat model first.")
        val settings = snapshot.chatSettings.toEffective()
        val visibleMessages = if (appendUser) base + ChatMessage.user(userText) else base
        val startedAt = System.currentTimeMillis()
        val runId = UUID.randomUUID().toString()
        val model = snapshot.chatModels.firstOrNull { it.id == selectedId }
        var assistantId: String? = null
        try {
            state.update {
                it.copy(
                    input = if (appendUser) "" else it.input,
                    messages = visibleMessages,
                    operation = AssistantOperation.Loading,
                    errorMessage = null,
                    statusMessage = "Loading the local chat model…",
                    metrics = null,
                )
            }
            var completedText = ""
            generateResponse.execute(
                ChatGenerationRequest(selectedId, visibleMessages.map(ChatMessage::toDomain), settings.toDomain(), runId),
            ).collect { event ->
                when (event) {
                    is ChatGenerationEvent.Prepared -> {
                        assistantId = UUID.randomUUID().toString()
                        state.update {
                            it.copy(
                                operation = AssistantOperation.Generating,
                                statusMessage = "Generating locally…",
                                contextUsage = event.contextUsage.toUi(),
                                messages = visibleMessages + ChatMessage.assistant(requireNotNull(assistantId), "", true),
                            )
                        }
                    }
                    is ChatGenerationEvent.Token -> assistantId?.let { id -> state.update { it.replaceAssistantText(id, event.text, append = true) } }
                    is ChatGenerationEvent.Completed -> {
                        val id = requireNotNull(assistantId) { "The chat engine completed without preparing a response." }
                        val result = event.generation
                        completedText = result.text
                        val metrics = ChatMetrics(
                            event.modelName,
                            event.load.coldStart,
                            event.load.loadDurationMs,
                            result.promptTokenCount,
                            rate(result.promptTokenCount, result.promptDurationMs),
                            result.firstTokenLatencyMs,
                            result.generatedTokenCount,
                            rate(result.generatedTokenCount, result.generationDurationMs),
                            result.totalDurationMs,
                            result.finishReason,
                            settings,
                            event.load.diagnostics.effectiveThreadCount,
                            event.telemetry,
                        )
                        state.update { current ->
                            current.replaceAssistantText(id, result.text, append = false).copy(
                                operation = AssistantOperation.Idle,
                                metrics = metrics,
                                statusMessage = null,
                            )
                        }
                        activeLinkedRunIds += persistAssistantTurn(
                            AssistantChatPersistenceSnapshotFactory.create(
                                runId, conversationId(), RunStatus.SUCCEEDED, startedAt, model, userText, result.text, settings, metrics,
                                null, state.value.messages,
                            ),
                        )
                    }
                }
            }
            val speech = if (speakAfter) speakInternal(completedText, requireNotNull(assistantId)) else SpeechOutcome()
            return GenerationOutcome(completedText, speech.succeeded, speech.error)
        } catch (cancelled: CancellationException) {
            state.update { current ->
                current.copy(
                    operation = AssistantOperation.Idle,
                    speakingMessageId = null,
                    statusMessage = "Generation stopped.",
                    messages = current.messages.map { if (it.streaming) it.copy(streaming = false, failed = true) else it },
                )
            }
            val partial = state.value.messages.lastOrNull { it.role == ChatMessageRole.ASSISTANT }?.content
            withContext(NonCancellable) {
                activeLinkedRunIds += persistAssistantTurn(
                    AssistantChatPersistenceSnapshotFactory.create(
                        runId, conversationId(), RunStatus.CANCELLED, startedAt, model, userText, partial, settings, null,
                        "Generation cancelled.", state.value.messages, incompleteAssistant = true,
                    ),
                )
            }
            throw cancelled
        } catch (error: Throwable) {
            val message = error.message ?: "Local generation failed."
            state.update { current ->
                current.copy(
                    operation = AssistantOperation.Idle,
                    speakingMessageId = null,
                    errorMessage = message,
                    messages = current.messages.map { if (it.streaming) it.copy(streaming = false, failed = true) else it },
                )
            }
            withContext(NonCancellable) {
                activeLinkedRunIds += persistAssistantTurn(
                    AssistantChatPersistenceSnapshotFactory.create(
                        runId, conversationId(), RunStatus.FAILED, startedAt, model, userText, null, settings, null,
                        message, state.value.messages, incompleteAssistant = true,
                    ),
                )
            }
            throw error
        }
    }

    private suspend fun speakInternal(text: String, messageId: String): SpeechOutcome {
        val snapshot = state.value
        val model = snapshot.selectedVoiceModel
        val voice = snapshot.selectedVoice
        if (model == null || voice == null || !model.installed) {
            return SpeechOutcome(succeeded = false, error = "Configure text-to-speech before speaking responses.")
        }
        val startedAt = System.currentTimeMillis()
        var metrics: SpeechSynthesisMetrics? = null
        return try {
            state.update {
                it.copy(
                    operation = AssistantOperation.Speaking,
                    speakingMessageId = messageId,
                    statusMessage = "Speaking with ${voice.displayName}…",
                    errorMessage = null,
                )
            }
            speechOutput.speak(model.id, model, voice, text, snapshot.speechOutputSettings).collect { event ->
                if (event is SpeechSynthesisEvent.Completed) metrics = event.metrics
            }
            activeLinkedRunIds += runRecorder.recordSpeechOutput(
                status = RunStatus.SUCCEEDED,
                startedAtEpochMs = startedAt,
                model = model.toRunSnapshot(),
                text = text,
                languageCode = snapshot.speechOutputSettings.languageCode,
                voiceId = voice.id,
                metrics = metrics,
                error = null,
            )
            state.update { it.copy(operation = AssistantOperation.Idle, speakingMessageId = null, statusMessage = null) }
            SpeechOutcome(succeeded = true)
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                activeLinkedRunIds += runRecorder.recordSpeechOutput(
                    status = RunStatus.CANCELLED,
                    startedAtEpochMs = startedAt,
                    model = model.toRunSnapshot(),
                    text = text,
                    languageCode = snapshot.speechOutputSettings.languageCode,
                    voiceId = voice.id,
                    metrics = metrics,
                    error = "Speech playback cancelled.",
                )
            }
            state.update { it.copy(operation = AssistantOperation.Idle, speakingMessageId = null, statusMessage = "Speech stopped.") }
            throw cancelled
        } catch (error: Throwable) {
            val message = error.message ?: "Could not speak this response."
            withContext(NonCancellable) {
                activeLinkedRunIds += runRecorder.recordSpeechOutput(
                    status = RunStatus.FAILED,
                    startedAtEpochMs = startedAt,
                    model = model.toRunSnapshot(),
                    text = text,
                    languageCode = snapshot.speechOutputSettings.languageCode,
                    voiceId = voice.id,
                    metrics = metrics,
                    error = message,
                )
            }
            state.update { it.copy(operation = AssistantOperation.Idle, speakingMessageId = null, errorMessage = message, statusMessage = null) }
            SpeechOutcome(succeeded = false, error = message)
        }
    }

    private fun launchForeground(block: suspend () -> Unit) {
        if (activeJob?.isActive == true) return
        activeLinkedRunIds.clear()
        val job = scope.launch(Dispatchers.Default) { block() }
        activeJob = job
        val registration = operationCoordinator.register(::cancel)
        job.invokeOnCompletion {
            registration.close()
            if (activeJob === job) activeJob = null
        }
    }

    private fun handleOperationFailure(error: Throwable) {
        if (error is CancellationException) return
        Log.e(TAG, "Assistant operation failed: ${error.message}", error)
        state.update {
            it.copy(
                operation = AssistantOperation.Idle,
                level = null,
                speakingMessageId = null,
                errorMessage = error.message ?: "The local assistant operation failed.",
            )
        }
    }

    private fun SpeechModelOption.toRunSnapshot() = RunModelSnapshot(id.value, displayName, engineId.value)

    private fun TtsModelOption.toRunSnapshot() = RunModelSnapshot(id.value, displayName, engineId.value)

    private fun appendTranscript(existing: String, transcript: String): String = when {
        existing.isBlank() -> transcript
        existing.last().isWhitespace() -> existing + transcript
        else -> "$existing $transcript"
    }

    private fun rate(tokens: Int?, durationMs: Long): Double? = tokens?.let { count ->
        durationMs.takeIf { it > 0 }?.let { count * 1_000.0 / it }
    }

    private companion object {
        const val TAG = "AiP123Assistant"
    }
}

private data class GenerationOutcome(
    val text: String,
    val speechSucceeded: Boolean,
    val speechError: String?,
)

private data class SpeechOutcome(
    val succeeded: Boolean = true,
    val error: String? = null,
)
