package com.dmitriim.localaiplayground.feature.voice.domain

import android.os.SystemClock
import com.dmitriim.localaiplayground.ai.api.ChatEngine
import com.dmitriim.localaiplayground.ai.api.SpeechToTextEngine
import com.dmitriim.localaiplayground.ai.api.TextToSpeechEngine
import com.dmitriim.localaiplayground.core.audio.input.model.PcmAudioInput
import com.dmitriim.localaiplayground.core.audio.input.storage.AudioInputStore
import com.dmitriim.localaiplayground.core.audio.output.api.StreamingSpeechPlayer
import com.dmitriim.localaiplayground.core.model.LocalModelResolver
import dev.zacsweers.metro.Inject
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn

/** Owns one foreground turn's sequencing, cancellation, and cleanup. */
@Inject
class VoiceAssistantCoordinator(
    private val modelResolver: LocalModelResolver,
    private val audioInputStore: AudioInputStore,
    private val speechEngine: SpeechToTextEngine,
    private val chatEngine: ChatEngine,
    private val textToSpeechEngine: TextToSpeechEngine,
    private val player: StreamingSpeechPlayer,
) {
    private val cancelled = AtomicBoolean(false)

    fun execute(request: VoiceTurnRequest): Flow<VoicePipelineEvent> = channelFlow {
        cancelled.set(false)
        val anchorNanos = System.nanoTime()
        val componentIds = VoiceComponentRunIds.newIds()
        var input: PcmAudioInput? = null
        var playbackOpen = false
        try {
            val prepared = modelResolver.preflightVoicePipeline(request)
            trySend(VoicePipelineEvent.Prepared(prepared.toInfo(), componentIds))
            checkNotCancelled()

            trySend(VoicePipelineEvent.Phase(VoiceTurnPhase.LISTENING))
            input = audioInputStore.capture(prepared.speech.sampleRateHz) { level ->
                trySend(VoicePipelineEvent.Level(level))
            }
            checkNotCancelled()

            trySend(VoicePipelineEvent.Phase(VoiceTurnPhase.FINALIZING))
            val finalizationStartedMs = SystemClock.elapsedRealtime()
            val transcription = transcribeVoiceInput(
                audioInputStore = audioInputStore,
                engine = speechEngine,
                input = input,
                model = prepared.speech,
                languageCode = request.languageCode,
                threadCount = request.sttThreadCount,
                ensureNotCancelled = ::checkNotCancelled,
            )
            val speechFinalizationDurationMs = SystemClock.elapsedRealtime() - finalizationStartedMs
            require(transcription.text.isNotBlank()) { "No speech was recognized. Record another turn and try again." }
            trySend(VoicePipelineEvent.FinalTranscript(transcription.text))
            checkNotCancelled()

            trySend(VoicePipelineEvent.Phase(VoiceTurnPhase.THINKING))
            val response = generateVoiceResponse(
                engine = chatEngine,
                request = request,
                model = prepared.chat,
                userText = transcription.text,
                onPrepared = { usage -> trySend(VoicePipelineEvent.ContextPrepared(usage)) },
                onToken = { token -> trySend(VoicePipelineEvent.AssistantToken(token)) },
                ensureNotCancelled = ::checkNotCancelled,
            )
            trySend(VoicePipelineEvent.AssistantCompleted(response.text))
            checkNotCancelled()

            trySend(VoicePipelineEvent.Phase(VoiceTurnPhase.SPEAKING))
            val speechMetrics = synthesizeAndPlayVoiceResponse(
                engine = textToSpeechEngine,
                player = player,
                text = response.text,
                request = request,
                model = prepared.voice,
                anchorNanos = anchorNanos,
                isCancelled = cancelled::get,
                ensureNotCancelled = ::checkNotCancelled,
                onPlaybackOpened = { playbackOpen = true },
            )
            playbackOpen = false
            trySend(
                VoicePipelineEvent.Completed(
                    VoicePipelineMetrics(
                        listeningDurationMs = input.durationMs,
                        speechFinalizationDurationMs = speechFinalizationDurationMs,
                        sttProcessingDurationMs = transcription.processingDurationMs,
                        llmTimeToFirstTokenMs = response.llmTimeToFirstTokenMs,
                        llmCompletionDurationMs = response.llmCompletionDurationMs,
                        ttsTimeToFirstChunkMs = speechMetrics.timeToFirstChunkMs,
                        ttsTimeToFirstWriteMs = speechMetrics.timeToFirstWriteMs,
                        ttsTimeToFirstPresentationMs = speechMetrics.timeToFirstPresentationMs,
                        ttsCompletionDurationMs = speechMetrics.completionDurationMs,
                        endToEndTimeToFirstOutputMs = speechMetrics.timeToFirstPresentationMs
                            ?: speechMetrics.timeToFirstWriteMs,
                        sttModelName = prepared.speech.displayName,
                        chatModelName = prepared.chat.displayName,
                        voiceModelName = prepared.voice.displayName,
                        componentRunIds = componentIds,
                    ),
                ),
            )
        } finally {
            audioInputStore.clear(input)
            runCatching { speechEngine.unload() }
            runCatching { chatEngine.unload() }
            runCatching { textToSpeechEngine.unload() }
            if (playbackOpen) player.release(completed = false)
        }
    }.flowOn(Dispatchers.Default)

    fun stopListening() = audioInputStore.stopCapture()

    fun cancel() {
        cancelled.set(true)
        audioInputStore.stopCapture()
        speechEngine.cancel()
        chatEngine.cancel()
        textToSpeechEngine.cancel()
        player.stop()
    }

    private fun checkNotCancelled() {
        if (cancelled.get()) throw CancellationException("Voice turn cancelled")
    }
}
