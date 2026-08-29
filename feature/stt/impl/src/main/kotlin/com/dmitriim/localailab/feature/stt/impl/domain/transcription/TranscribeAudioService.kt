package com.dmitriim.localailab.feature.stt.impl.domain.transcription

import android.os.SystemClock
import android.util.Log
import com.dmitriim.localailab.ai.api.stt.SpeechToTextEngine
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.ai.api.stt.SpeechToTextRequest
import com.dmitriim.localailab.core.audio.input.storage.AudioInputStore
import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.feature.models.api.domain.runtime.LocalModelResolver
import com.dmitriim.localailab.core.performance.profiling.InferencePhase
import com.dmitriim.localailab.core.performance.profiling.InferenceProfiler
import com.dmitriim.localailab.core.performance.profiling.LightweightInferenceProfiler
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.feature.stt.api.domain.SpeechTranscriptionEvent
import com.dmitriim.localailab.feature.stt.api.domain.SpeechTranscriptionMetrics
import com.dmitriim.localailab.feature.stt.api.domain.SpeechTranscriptionRequest
import com.dmitriim.localailab.feature.stt.api.domain.TranscribeAudio
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/** Runs one complete local transcription and reports domain events. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<TranscribeAudio>())
class TranscribeAudioService(
    private val modelResolver: LocalModelResolver,
    private val speechEngine: SpeechToTextEngine,
    private val audioInputStore: AudioInputStore,
    private val profiler: InferenceProfiler = LightweightInferenceProfiler,
) : TranscribeAudio {
    override fun execute(request: SpeechTranscriptionRequest): Flow<SpeechTranscriptionEvent> = flow {
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
            collectResourceTelemetry = request.extendedProfiling,
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
                        artifacts = model.artifacts,
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

            val segments = transcribeSegments(request, profile)
            val totalDurationMs = SystemClock.elapsedRealtime() - startedAt
            Log.i(
                TAG,
                "STT transcription completed: segments=${segments.count}, transcriptLength=${segments.transcript.length}, " +
                    "processingMs=${segments.processingDurationMs}, totalMs=$totalDurationMs",
            )
            val telemetry = profile.finish()
            emit(
                SpeechTranscriptionEvent.Completed(
                    transcript = segments.transcript,
                    metrics = SpeechTranscriptionMetrics(
                        audioDurationMs = request.input.durationMs,
                        processingDurationMs = segments.processingDurationMs,
                        timeToFinalMs = totalDurationMs,
                        realTimeFactor = request.input.durationMs.takeIf { it > 0 }?.let { totalDurationMs.toDouble() / it },
                        segmentCount = segments.count,
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

    override fun cancel() {
        Log.i(TAG, "STT cancellation requested.")
        speechEngine.cancel()
    }

    override fun unload() = speechEngine.unload()

    private suspend fun transcribeSegments(
        request: SpeechTranscriptionRequest,
        profile: com.dmitriim.localailab.core.performance.profiling.InferenceProfileSession,
    ): TranscribedSegments {
        val transcript = StringBuilder()
        var count = 0
        var processingDurationMs = 0L
        profile.trace(InferencePhase.TRANSCRIPTION) {
            audioInputStore.forEachSegment(request.input) { samples ->
                val number = count + 1
                Log.i(TAG, "STT segment started: number=$number, samples=${samples.size}, durationMs=${samples.size * 1_000L / request.input.sampleRateHz}")
                val result = speechEngine.transcribe(SpeechToTextRequest(samples, request.input.sampleRateHz))
                if (result.text.isNotBlank()) {
                    if (transcript.isNotEmpty()) transcript.append(' ')
                    transcript.append(result.text)
                }
                count++
                processingDurationMs += result.processingDurationMs
                Log.i(TAG, "STT segment completed: number=$number, processingMs=${result.processingDurationMs}, transcriptLength=${result.text.length}")
            }
        }
        return TranscribedSegments(transcript.toString(), count, processingDurationMs)
    }

    private companion object {
        const val TAG = "AiP123Stt"
    }
}

private data class TranscribedSegments(
    val transcript: String,
    val count: Int,
    val processingDurationMs: Long,
)
