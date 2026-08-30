package com.dmitriim.localailab.feature.assistant.impl.presentation

import android.app.Application
import android.util.Log
import com.dmitriim.localailab.ai.api.chat.ChatEngine
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.core.operation.ForegroundOperationCoordinator
import com.dmitriim.localailab.core.operation.ForegroundOperationInterruption
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.feature.assistant.impl.domain.AssistantRunRecorder
import com.dmitriim.localailab.feature.assistant.impl.domain.chat.GenerateAssistantResponse
import com.dmitriim.localailab.feature.assistant.impl.domain.chat.PersistAssistantTurn
import com.dmitriim.localailab.feature.assistant.impl.domain.stt.AssistantAudioRecorder
import com.dmitriim.localailab.feature.assistant.impl.domain.stt.AssistantTranscriber
import com.dmitriim.localailab.feature.assistant.impl.domain.tts.AssistantSpeechOutput
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.AssistantOperation
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.AssistantUiState
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.SpeechOutputSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Routes assistant actions to the focused chat, STT, and TTS controllers. */
internal class AssistantOperationController(
    private val application: Application,
    override val scope: CoroutineScope,
    override val state: MutableStateFlow<AssistantUiState>,
    chatEngine: ChatEngine,
    generateResponse: GenerateAssistantResponse,
    audioRecorder: AssistantAudioRecorder,
    transcriber: AssistantTranscriber,
    speechOutput: AssistantSpeechOutput,
    runRecorder: AssistantRunRecorder,
    persistAssistantTurn: PersistAssistantTurn,
    private val operationCoordinator: ForegroundOperationCoordinator,
    conversationId: () -> String,
) : AssistantOperationHost {
    override val activeLinkedRunIds = mutableListOf<String>()

    private var activeJob: Job? = null

    private val ttsController = AssistantTtsController(
        host = this,
        speechOutput = speechOutput,
        runRecorder = runRecorder,
        application = application,
    )
    private val chatController = AssistantChatController(
        host = this,
        chatEngine = chatEngine,
        generateResponse = generateResponse,
        persistAssistantTurn = persistAssistantTurn,
        ttsController = ttsController,
        conversationId = conversationId,
        application = application,
    )
    private val sttController = AssistantSttController(
        host = this,
        audioRecorder = audioRecorder,
        transcriber = transcriber,
        runRecorder = runRecorder,
        chatController = chatController,
        application = application,
    )

    fun unloadChatRuntime() = chatController.unloadRuntime()

    fun unloadSpeechOutputRuntime() = ttsController.unloadRuntime()

    fun send() = chatController.send()

    fun startRecording() = sttController.startRecording()

    fun stopRecording() = sttController.stopRecording()

    fun speakMessage(messageId: String) = ttsController.speakMessage(messageId)

    fun previewVoice(
        modelId: ModelId,
        voiceId: String,
        settings: SpeechOutputSettings,
    ): String? = ttsController.previewVoice(modelId, voiceId, settings)

    fun regenerate() = chatController.regenerate()

    fun cancel() {
        if (activeJob?.isActive != true) return
        state.update {
            it.copy(
                operation = AssistantOperation.Cancelling,
                statusMessage = application.getString(CoreUiR.string.assistant_operation_stopping),
            )
        }
        sttController.cancel()
        chatController.cancel()
        ttsController.cancel()
        activeJob?.cancel(CancellationException("Assistant operation cancelled"))
    }

    fun clear() {
        sttController.cancel()
        chatController.cancel()
        ttsController.cancel()
        activeJob?.cancel()
    }

    override fun launchForeground(block: suspend () -> Unit) {
        if (activeJob?.isActive == true) return
        activeLinkedRunIds.clear()
        val job = scope.launch(Dispatchers.Default) { block() }
        activeJob = job
        val registration = operationCoordinator.registerInterruptionHandler { interruption ->
            when (interruption) {
                ForegroundOperationInterruption.APP_BACKGROUNDED -> cancel()
                ForegroundOperationInterruption.MEMORY_PRESSURE -> interruptForMemoryPressure()
            }
        }
        job.invokeOnCompletion {
            registration.close()
            if (activeJob === job) activeJob = null
        }
    }

    override fun handleOperationFailure(error: Throwable) {
        if (error is CancellationException) return
        Log.e(TAG, "Assistant operation failed: ${error.message}", error)
        state.update {
            it.copy(
                operation = AssistantOperation.Idle,
                level = null,
                speakingMessageId = null,
                errorMessage = error.message
                    ?: application.getString(CoreUiR.string.assistant_error_operation_failed),
            )
        }
    }

    private fun interruptForMemoryPressure() {
        if (activeJob?.isActive != true) return
        state.update {
            it.copy(
                errorMessage = application.getString(CoreUiR.string.assistant_error_memory_pressure),
            )
        }
        cancel()
    }

    private companion object {
        const val TAG = "AiP123Assistant"
    }
}
