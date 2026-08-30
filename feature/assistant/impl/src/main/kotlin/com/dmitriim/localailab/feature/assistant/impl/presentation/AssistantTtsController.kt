package com.dmitriim.localailab.feature.assistant.impl.presentation

import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.feature.assistant.impl.domain.AssistantRunRecorder
import com.dmitriim.localailab.feature.assistant.impl.domain.tts.AssistantSpeechOutput
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.AssistantOperation
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.ChatMessageRole
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.SpeechOutcome
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.SpeechOutputSettings
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.TtsModelOption
import com.dmitriim.localailab.feature.runs.api.domain.history.RunModelSnapshot
import com.dmitriim.localailab.feature.runs.api.domain.history.RunStatus
import com.dmitriim.localailab.feature.tts.api.domain.SpeechSynthesisEvent
import com.dmitriim.localailab.feature.tts.api.domain.SpeechSynthesisMetrics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class AssistantTtsController(
    private val host: AssistantOperationHost,
    private val speechOutput: AssistantSpeechOutput,
    private val runRecorder: AssistantRunRecorder,
) {
    fun unloadRuntime() {
        host.scope.launch(Dispatchers.Default) { speechOutput.unload() }
    }

    fun speakMessage(messageId: String) {
        val snapshot = host.state.value
        if (!snapshot.isIdle) return
        val message = snapshot.messages.firstOrNull {
            it.id == messageId && it.role == ChatMessageRole.ASSISTANT && !it.streaming && it.content.isNotBlank()
        } ?: return
        host.launchForeground {
            val outcome = speakInternal(message.content, message.id)
            if (!outcome.succeeded && outcome.error != null) {
                host.state.update { it.copy(errorMessage = outcome.error) }
            }
        }
    }

    fun previewVoice(
        modelId: ModelId,
        voiceId: String,
        settings: SpeechOutputSettings,
    ): String? {
        val snapshot = host.state.value
        if (!snapshot.isIdle) return "Wait for the current operation to finish."
        val model = snapshot.voiceModels.firstOrNull { it.id == modelId && it.installed }
            ?: return "Select an installed text-to-speech model."
        val validationError = runCatching(settings::validate).exceptionOrNull()?.message
        if (validationError != null) return validationError
        val voice = model.compatibleVoices(settings.languageCode).firstOrNull { it.id == voiceId }
            ?: return "Select a voice compatible with this language."
        host.launchForeground {
            try {
                host.state.update {
                    it.copy(
                        operation = AssistantOperation.Speaking,
                        statusMessage = "Previewing ${voice.displayName}…",
                        errorMessage = null,
                    )
                }
                speechOutput.preview(model.id, model, voice, snapshot.speechOutputSettings)
                host.state.update {
                    it.copy(
                        operation = AssistantOperation.Idle,
                        statusMessage = "Voice preview completed.",
                    )
                }
            } catch (_: CancellationException) {
                host.state.update {
                    it.copy(
                        operation = AssistantOperation.Idle,
                        statusMessage = "Voice preview stopped.",
                    )
                }
            } catch (error: Throwable) {
                host.state.update {
                    it.copy(
                        operation = AssistantOperation.Idle,
                        errorMessage = error.message ?: "Could not preview this voice.",
                    )
                }
            }
        }
        return null
    }

    suspend fun speakInternal(text: String, messageId: String): SpeechOutcome {
        val snapshot = host.state.value
        val model = snapshot.selectedVoiceModel
        val voice = snapshot.selectedVoice
        if (model == null || voice == null || !model.installed) {
            return SpeechOutcome(
                succeeded = false,
                error = "Configure text-to-speech before speaking responses.",
            )
        }
        val startedAt = System.currentTimeMillis()
        var metrics: SpeechSynthesisMetrics? = null
        return try {
            host.state.update {
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
            host.activeLinkedRunIds += runRecorder.recordSpeechOutput(
                status = RunStatus.SUCCEEDED,
                startedAtEpochMs = startedAt,
                model = model.toRunSnapshot(),
                text = text,
                languageCode = snapshot.speechOutputSettings.languageCode,
                voiceId = voice.id,
                metrics = metrics,
                error = null,
            )
            host.state.update {
                it.copy(
                    operation = AssistantOperation.Idle,
                    speakingMessageId = null,
                    statusMessage = null,
                )
            }
            SpeechOutcome(succeeded = true)
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                host.activeLinkedRunIds += runRecorder.recordSpeechOutput(
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
            host.state.update {
                it.copy(
                    operation = AssistantOperation.Idle,
                    speakingMessageId = null,
                    statusMessage = "Speech stopped.",
                )
            }
            throw cancelled
        } catch (error: Throwable) {
            val message = error.message ?: "Could not speak this response."
            withContext(NonCancellable) {
                host.activeLinkedRunIds += runRecorder.recordSpeechOutput(
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
            host.state.update {
                it.copy(
                    operation = AssistantOperation.Idle,
                    speakingMessageId = null,
                    errorMessage = message,
                    statusMessage = null,
                )
            }
            SpeechOutcome(succeeded = false, error = message)
        }
    }

    fun cancel() {
        speechOutput.cancel()
    }

    private fun TtsModelOption.toRunSnapshot() = RunModelSnapshot(id.value, displayName, engineId.value)
}
