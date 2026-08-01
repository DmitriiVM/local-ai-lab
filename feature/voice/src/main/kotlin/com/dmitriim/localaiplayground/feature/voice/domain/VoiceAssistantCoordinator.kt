package com.dmitriim.localaiplayground.feature.voice.domain

import android.os.SystemClock
import android.util.Log
import com.dmitriim.localaiplayground.ai.api.llm.ChatEngine
import com.dmitriim.localaiplayground.ai.api.stt.SpeechToTextEngine
import com.dmitriim.localaiplayground.ai.api.tts.TextToSpeechEngine
import com.dmitriim.localaiplayground.core.audio.input.model.PcmAudioInput
import com.dmitriim.localaiplayground.core.audio.input.storage.AudioInputStore
import com.dmitriim.localaiplayground.core.audio.output.api.StreamingSpeechPlayer
import com.dmitriim.localaiplayground.core.model.service.LocalModelResolver
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.atomic.AtomicBoolean

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
        Log.i(
            TAG,
            "Voice turn requested: language=${request.languageCode}, historyTurns=${request.history.size}, " +
                "contextSize=${request.contextSize}, maxOutputTokens=${request.maxOutputTokens}, " +
                "temperature=${request.temperature}, sttThreads=${request.sttThreadCount}, " +
                "llmThreads=${request.llmThreadCount}, ttsThreads=${request.ttsThreadCount}, " +
                "speaker=${request.speakerId}, speechRate=${request.speechRate}, volume=${request.volume}",
        )
        val anchorNanos = System.nanoTime()
        val componentIds = VoiceComponentRunIds.newIds()
        var input: PcmAudioInput? = null
        var playbackOpen = false
        try {
            val prepared = modelResolver.preflightVoicePipeline(request)
            Log.i(TAG, "Voice pipeline preflight succeeded: stt=${prepared.speech.displayName}, chat=${prepared.chat.displayName}, tts=${prepared.voice.displayName}")
            trySend(VoicePipelineEvent.Prepared(prepared.toInfo(), componentIds))
            checkNotCancelled()

            trySend(VoicePipelineEvent.Phase(VoiceTurnPhase.LISTENING))
            Log.i(TAG, "Voice phase started: LISTENING")
            input = audioInputStore.capture(prepared.speech.sampleRateHz) { level ->
                trySend(VoicePipelineEvent.Level(level))
            }
            checkNotCancelled()
            Log.i(TAG, "Voice capture completed: durationMs=${input.durationMs}, sampleRateHz=${input.sampleRateHz}")

            trySend(VoicePipelineEvent.Phase(VoiceTurnPhase.FINALIZING))
            Log.i(TAG, "Voice phase started: FINALIZING")
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
            Log.i(TAG, "Voice transcription completed: transcriptLength=${transcription.text.length}, processingMs=${transcription.processingDurationMs}, finalizationMs=$speechFinalizationDurationMs")
            trySend(VoicePipelineEvent.FinalTranscript(transcription.text))
            checkNotCancelled()

            trySend(VoicePipelineEvent.Phase(VoiceTurnPhase.THINKING))
            Log.i(TAG, "Voice phase started: THINKING")
            val response = generateVoiceResponse(
                engine = chatEngine,
                request = request,
                model = prepared.chat,
                userText = transcription.text,
                onPrepared = { usage -> trySend(VoicePipelineEvent.ContextPrepared(usage)) },
                onToken = { token -> trySend(VoicePipelineEvent.AssistantToken(token)) },
                ensureNotCancelled = ::checkNotCancelled,
            )
            Log.i(TAG, "Voice response generation completed: responseLength=${response.text.length}, firstTokenMs=${response.llmTimeToFirstTokenMs}, completionMs=${response.llmCompletionDurationMs}")
            trySend(VoicePipelineEvent.AssistantCompleted(response.text))
            checkNotCancelled()

            trySend(VoicePipelineEvent.Phase(VoiceTurnPhase.SPEAKING))
            Log.i(TAG, "Voice phase started: SPEAKING")
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
            Log.i(
                TAG,
                "Voice speech completed: firstChunkMs=${speechMetrics.timeToFirstChunkMs}, " +
                    "firstWriteMs=${speechMetrics.timeToFirstWriteMs}, " +
                    "firstPresentationMs=${speechMetrics.timeToFirstPresentationMs}, " +
                    "completionMs=${speechMetrics.completionDurationMs}",
            )
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
            Log.i(TAG, "Voice turn completed successfully: listeningMs=${input.durationMs}, endToEndFirstOutputMs=${speechMetrics.timeToFirstPresentationMs ?: speechMetrics.timeToFirstWriteMs}")
        } catch (error: Throwable) {
            Log.e(TAG, "Voice turn failed: ${error.message}", error)
            throw error
        } finally {
            audioInputStore.clear(input)
            runCatching { speechEngine.unload() }
            runCatching { chatEngine.unload() }
            runCatching { textToSpeechEngine.unload() }
            if (playbackOpen) player.release(completed = false)
            Log.i(TAG, "Voice turn cleanup completed: cancelled=${cancelled.get()}, playbackOpen=$playbackOpen")
        }
    }.flowOn(Dispatchers.Default)

    fun stopListening() {
        Log.i(TAG, "Voice stop-listening requested.")
        audioInputStore.stopCapture()
    }

    fun cancel() {
        Log.i(TAG, "Voice cancellation requested: stopping capture, STT, LLM, TTS, and playback.")
        cancelled.set(true)
        audioInputStore.stopCapture()
        speechEngine.cancel()
        chatEngine.cancel()
        textToSpeechEngine.cancel()
        player.stop()
    }

    private fun checkNotCancelled() {
        if (cancelled.get()) {
            Log.i(TAG, "Voice cancellation observed by pipeline.")
            throw CancellationException("Voice turn cancelled")
        }
    }

    private companion object {
        const val TAG = "AiP123Voice"
    }
}
