package com.dmitriim.localailab.feature.assistant.impl.presentation

import android.app.Application
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.core.ui.R as CoreUiR
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
    private val application: Application,
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
        if (!snapshot.isIdle) {
            return application.getString(CoreUiR.string.assistant_error_operation_active)
        }
        val model = snapshot.voiceModels.firstOrNull { it.id == modelId && it.installed }
            ?: return application.getString(CoreUiR.string.assistant_error_select_tts_model)
        val validationError = runCatching(settings::validate).exceptionOrNull()?.message
        if (validationError != null) return validationError
        val voice = model.compatibleVoices(settings.languageCode).firstOrNull { it.id == voiceId }
            ?: return application.getString(CoreUiR.string.assistant_error_select_compatible_voice)
        host.launchForeground {
            try {
                host.state.update {
                    it.copy(
                        operation = AssistantOperation.Speaking,
                        statusMessage = application.getString(
                            CoreUiR.string.assistant_status_previewing_voice,
                            voice.displayName,
                        ),
                        errorMessage = null,
                    )
                }
                speechOutput.preview(model.id, model, voice, snapshot.speechOutputSettings)
                host.state.update {
                    it.copy(
                        operation = AssistantOperation.Idle,
                        statusMessage = application.getString(CoreUiR.string.assistant_status_voice_preview_completed),
                    )
                }
            } catch (_: CancellationException) {
                host.state.update {
                    it.copy(
                        operation = AssistantOperation.Idle,
                        statusMessage = application.getString(CoreUiR.string.assistant_status_voice_preview_stopped),
                    )
                }
            } catch (error: Throwable) {
                host.state.update {
                    it.copy(
                        operation = AssistantOperation.Idle,
                        errorMessage = error.message
                            ?: application.getString(CoreUiR.string.assistant_error_preview_voice),
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
                error = application.getString(CoreUiR.string.assistant_error_configure_text_to_speech),
            )
        }
        val startedAt = System.currentTimeMillis()
        var metrics: SpeechSynthesisMetrics? = null
        return try {
            host.state.update {
                it.copy(
                    operation = AssistantOperation.Speaking,
                    speakingMessageId = messageId,
                    statusMessage = application.getString(
                        CoreUiR.string.assistant_status_speaking_with_voice,
                        voice.displayName,
                    ),
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
                    error = application.getString(CoreUiR.string.assistant_error_speech_playback_cancelled),
                )
            }
            host.state.update {
                it.copy(
                    operation = AssistantOperation.Idle,
                    speakingMessageId = null,
                    statusMessage = application.getString(CoreUiR.string.assistant_status_speech_stopped),
                )
            }
            throw cancelled
        } catch (error: Throwable) {
            val message = error.message
                ?: application.getString(CoreUiR.string.assistant_error_speak_response)
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
