package com.dmitriim.localaiplayground.core.voice.stt

import android.os.SystemClock
import android.util.Log
import com.dmitriim.localaiplayground.ai.api.stt.SpeechToTextEngine
import com.dmitriim.localaiplayground.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localaiplayground.ai.api.stt.SpeechToTextRequest
import com.dmitriim.localaiplayground.core.audio.input.storage.AudioInputStore
import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.service.LocalModelResolver
import com.dmitriim.localaiplayground.core.performance.InferencePhase
import com.dmitriim.localaiplayground.core.performance.InferenceProfiler
import com.dmitriim.localaiplayground.core.performance.NoOpInferenceProfiler
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
    private val profiler: InferenceProfiler = NoOpInferenceProfiler,
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
        val profile = profiler.start(
            request.runId,
            AiCapability.SPEECH_TO_TEXT,
            extendedTelemetry = request.extendedProfiling,
        )
        try {
            val model = profile.trace(InferencePhase.MODEL_RESOLUTION) {
                modelResolver.resolveSpeechToTextModel(request.modelId).getOrThrow()
            }
            Log.i(TAG, "STT model resolved: name=${model.displayName}, directory=${model.modelDirectory}")
            val load = profile.trace(InferencePhase.MODEL_LOAD) {
                speechEngine.load(
                    SpeechToTextLoadRequest(
                        engineId = model.engineId,
                        profileType = model.profileType,
                        modelDirectory = model.modelDirectory,
                        files = model.files,
                        languageCode = effectiveSettings.languageCode,
                        threadCount = effectiveSettings.threadCount,
                    ),
                )
            }
            Log.i(
                TAG,
                "STT model loaded: coldStart=${load.coldStart}, loadMs=${load.loadDurationMs}, " +
                    "effectiveThreads=${load.effectiveThreadCount}",
            )
            emit(SpeechTranscriptionEvent.Prepared(model.displayName, load.loadDurationMs, load.effectiveThreadCount))

            val transcript = StringBuilder()
            var segmentCount = 0
            var processingDurationMs = 0L
            profile.trace(InferencePhase.TRANSCRIPTION) {
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
            }
            val totalDurationMs = SystemClock.elapsedRealtime() - startedAt
            Log.i(
                TAG,
                "STT transcription completed: segments=$segmentCount, transcriptLength=${transcript.length}, " +
                    "processingMs=$processingDurationMs, totalMs=$totalDurationMs",
            )
            val telemetry = profile.finish()
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
                        telemetry = telemetry,
                    ),
                ),
            )
        } catch (error: Throwable) {
            Log.e(TAG, "STT transcription flow failed: ${error.message}", error)
            throw error
        } finally {
            profile.finish()
            if (!request.keepLoaded) {
                runCatching { speechEngine.unload() }
                Log.i(TAG, "STT engine unloaded after transcription flow.")
            }
        }
    }.flowOn(Dispatchers.Default)

    fun cancel() {
        Log.i(TAG, "STT cancellation requested.")
        speechEngine.cancel()
    }

    fun unload() = speechEngine.unload()

    private companion object {
        const val TAG = "AiP123Stt"
    }
}
