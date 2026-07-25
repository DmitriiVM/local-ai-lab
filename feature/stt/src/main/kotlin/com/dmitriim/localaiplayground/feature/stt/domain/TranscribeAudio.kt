package com.dmitriim.localaiplayground.feature.stt.domain

import android.os.SystemClock
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

/** Runs one complete local Whisper transcription and reports domain events. */
@Inject
class TranscribeAudio(
    private val modelResolver: LocalModelResolver,
    private val speechEngine: SpeechToTextEngine,
    private val audioInputStore: AudioInputStore,
) {
    fun execute(request: SpeechTranscriptionRequest): Flow<SpeechTranscriptionEvent> = flow {
        val effectiveSettings = request.settings.toEffective()
        val startedAt = SystemClock.elapsedRealtime()
        try {
            val model = modelResolver.resolveSpeechToTextModel(request.modelId).getOrThrow()
            val load = speechEngine.load(
                SpeechToTextLoadRequest(
                    modelDirectory = model.modelDirectory,
                    languageCode = effectiveSettings.languageCode,
                    threadCount = effectiveSettings.threadCount,
                ),
            )
            emit(SpeechTranscriptionEvent.Prepared(model.displayName, load.loadDurationMs, load.effectiveThreadCount))

            val transcript = StringBuilder()
            var segmentCount = 0
            var processingDurationMs = 0L
            audioInputStore.forEachSegment(request.input) { samples ->
                val result = speechEngine.transcribe(SpeechToTextRequest(samples, request.input.sampleRateHz))
                if (result.text.isNotBlank()) {
                    if (transcript.isNotEmpty()) transcript.append(' ')
                    transcript.append(result.text)
                }
                segmentCount++
                processingDurationMs += result.processingDurationMs
            }
            val totalDurationMs = SystemClock.elapsedRealtime() - startedAt
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
        } finally {
            runCatching { speechEngine.unload() }
        }
    }.flowOn(Dispatchers.Default)

    fun cancel() = speechEngine.cancel()
}
