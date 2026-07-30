package com.dmitriim.localaiplayground.feature.stt.domain

import android.os.SystemClock
import android.util.Log
import com.dmitriim.localaiplayground.ai.api.SpeechToTextEngine
import com.dmitriim.localaiplayground.ai.api.SpeechToTextLoadRequest
import com.dmitriim.localaiplayground.ai.api.SpeechToTextRequest
import com.dmitriim.localaiplayground.core.audio.input.storage.AudioInputStore
import com.dmitriim.localaiplayground.core.model.LocalModelResolver
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/** Runs one complete local transcription and reports domain events. */
@Inject
class TranscribeAudio(
    private val modelResolver: LocalModelResolver,
    private val speechEngine: SpeechToTextEngine,
    private val audioInputStore: AudioInputStore,
) {
    fun execute(request: SpeechTranscriptionRequest): Flow<SpeechTranscriptionEvent> = flow {
        val effectiveSettings = request.settings.toEffective()
        Log.i(
            TAG,
            "STT transcription requested: modelId=${request.modelId.value}, inputDurationMs=${request.input.durationMs}, " +
                "sampleRateHz=${request.input.sampleRateHz}, language=${effectiveSettings.languageCode}, " +
                "requestedThreads=${effectiveSettings.threadCount}, source=${request.input.sourceDescription}",
        )
        val startedAt = SystemClock.elapsedRealtime()
        try {
            val model = modelResolver.resolveSpeechToTextModel(request.modelId).getOrThrow()
            Log.i(TAG, "STT model resolved: name=${model.displayName}, directory=${model.modelDirectory}")
            val load = speechEngine.load(
                SpeechToTextLoadRequest(
                    engineId = model.engineId,
                    profileType = model.profileType,
                    modelDirectory = model.modelDirectory,
                    files = model.files,
                    languageCode = effectiveSettings.languageCode,
                    threadCount = effectiveSettings.threadCount,
                ),
            )
            Log.i(
                TAG,
                "STT model loaded: coldStart=${load.coldStart}, loadMs=${load.loadDurationMs}, " +
                    "effectiveThreads=${load.effectiveThreadCount}",
            )
            emit(SpeechTranscriptionEvent.Prepared(model.displayName, load.loadDurationMs, load.effectiveThreadCount))

            val transcript = StringBuilder()
            var segmentCount = 0
            var processingDurationMs = 0L
            audioInputStore.forEachSegment(request.input) { samples ->
                val segmentNumber = segmentCount + 1
                Log.i(TAG, "STT segment started: number=$segmentNumber, samples=${samples.size}, durationMs=${samples.size * 1_000L / request.input.sampleRateHz}")
                val result = speechEngine.transcribe(SpeechToTextRequest(samples, request.input.sampleRateHz))
                if (result.text.isNotBlank()) {
                    if (transcript.isNotEmpty()) transcript.append(' ')
                    transcript.append(result.text)
                }
                segmentCount++
                processingDurationMs += result.processingDurationMs
                Log.i(TAG, "STT segment completed: number=$segmentNumber, processingMs=${result.processingDurationMs}, transcriptLength=${result.text.length}")
            }
            val totalDurationMs = SystemClock.elapsedRealtime() - startedAt
            Log.i(
                TAG,
                "STT transcription completed: segments=$segmentCount, transcriptLength=${transcript.length}, " +
                    "processingMs=$processingDurationMs, totalMs=$totalDurationMs",
            )
            emit(
                SpeechTranscriptionEvent.Completed(
                    transcript = transcript.toString(),
                    metrics = SpeechTranscriptionMetrics(
                        audioDurationMs = request.input.durationMs,
                        processingDurationMs = processingDurationMs,
                        timeToFinalMs = totalDurationMs,
                        realTimeFactor = request.input.durationMs.takeIf { it > 0 }?.let { totalDurationMs.toDouble() / it },
                        segmentCount = segmentCount,
                        loadDurationMs = load.loadDurationMs,
                        effectiveThreadCount = load.effectiveThreadCount,
                    ),
                ),
            )
        } catch (error: Throwable) {
            Log.e(TAG, "STT transcription flow failed: ${error.message}", error)
            throw error
        } finally {
            runCatching { speechEngine.unload() }
            Log.i(TAG, "STT engine unloaded after transcription flow.")
        }
    }.flowOn(Dispatchers.Default)

    fun cancel() {
        Log.i(TAG, "STT cancellation requested.")
        speechEngine.cancel()
    }

    private companion object {
        const val TAG = "AiP123Stt"
    }
}
