package com.dmitriim.localaiplayground.feature.tts.domain

import android.util.Log
import com.dmitriim.localaiplayground.ai.api.TextToSpeechEngine
import com.dmitriim.localaiplayground.ai.api.TextToSpeechLoadRequest
import com.dmitriim.localaiplayground.ai.api.TextToSpeechRequest
import com.dmitriim.localaiplayground.core.audio.output.api.StreamingSpeechPlayer
import com.dmitriim.localaiplayground.core.audio.output.model.GeneratedAudioFile
import com.dmitriim.localaiplayground.core.audio.output.storage.GeneratedAudioStore
import com.dmitriim.localaiplayground.core.audio.processing.SpeechAudioEffectsProcessor
import com.dmitriim.localaiplayground.core.model.LocalModelResolver
import dev.zacsweers.metro.Inject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/** Coordinates one bounded native synthesis stream, Android playback, and WAV retention. */
@Inject
class SynthesizeSpeech(
    private val modelResolver: LocalModelResolver,
    private val textToSpeechEngine: TextToSpeechEngine,
    private val player: StreamingSpeechPlayer,
    private val generatedAudioStore: GeneratedAudioStore,
    private val audioEffectsProcessor: SpeechAudioEffectsProcessor,
) {
    private val cancelled = AtomicBoolean(false)

    fun execute(request: SpeechSynthesisRequest): Flow<SpeechSynthesisEvent> = flow {
        Log.i(
            TAG,
            "TTS synthesis requested: modelId=${request.modelId.value}, textLength=${request.text.length}, " +
                "language=${request.settings.languageCode}, voice=${request.settings.voiceCondition}, " +
                "speed=${request.settings.speed}, silenceScale=${request.settings.sentenceSilenceScale}, " +
                "volume=${request.settings.volume}, requestedThreads=${request.settings.threadCount}, " +
                "audioEffects=${request.settings.audioEffects}",
        )
        request.settings.validate()
        require(request.text.isNotBlank()) { "Enter text to synthesize." }
        require(request.text.length <= MAX_TEXT_CHARACTERS) {
            "Text is limited to $MAX_TEXT_CHARACTERS characters for this playground."
        }
        cancelled.set(false)
        generatedAudioStore.discardPartial()
        val runAnchorNanos = System.nanoTime()
        var completed = false
        try {
            val model = modelResolver.resolveTextToSpeechModel(request.modelId).getOrThrow()
            Log.i(TAG, "TTS model resolved: name=${model.displayName}, directory=${model.modelDirectory}")
            val load = textToSpeechEngine.load(
                TextToSpeechLoadRequest(
                    engineId = model.engineId,
                    profileType = model.profileType,
                    modelDirectory = model.modelDirectory,
                    threadCount = request.settings.threadCount,
                ),
            )
            Log.i(
                TAG,
                "TTS model loaded: coldStart=${load.coldStart}, loadMs=${load.loadDurationMs}, " +
                    "threads=${load.effectiveThreadCount}, sampleRateHz=${load.sampleRateHz}, " +
                    "speakers=${load.speakerCount}",
            )
            val fixedSpeaker = request.settings.voiceCondition as?
                com.dmitriim.localaiplayground.ai.api.TextToSpeechVoiceCondition.FixedSpeaker
            if (fixedSpeaker != null) {
                require(
                    request.settings.expectedSpeakerCount?.let { it == load.speakerCount } != false &&
                        load.speakerCount?.let { fixedSpeaker.speakerId < it } == true,
                ) {
                    "${request.settings.voiceName ?: "The selected voice"} is unavailable in the installed ${model.displayName} bundle."
                }
            }
            emit(
                SpeechSynthesisEvent.Prepared(
                    modelName = model.displayName,
                    loadDurationMs = load.loadDurationMs,
                    effectiveThreadCount = load.effectiveThreadCount,
                    sampleRateHz = load.sampleRateHz,
                    speakerCount = load.speakerCount,
                ),
            )

            val effectsEnabled = !request.settings.audioEffects.isNeutral
            var session = if (effectsEnabled) {
                null
            } else {
                player.open(
                    sampleRateHz = load.sampleRateHz,
                    volume = request.settings.volume,
                    runAnchorNanos = runAnchorNanos,
                ).also {
                    Log.i(TAG, "TTS playback session opened: sampleRateHz=${load.sampleRateHz}")
                }
            }
            val chunks = session?.let { Channel<FloatArray>(capacity = AUDIO_QUEUE_CAPACITY) }
            var firstChunkNanos: Long? = null
            var chunkCount = 0
            var streamedSampleCount = 0L
            val playbackFailure = AtomicReference<Throwable?>(null)
            val consumer = session?.let { activeSession ->
                CoroutineScope(currentCoroutineContext()).launch(Dispatchers.IO) {
                    try {
                        for (chunk in requireNotNull(chunks)) {
                            if (!activeSession.write(chunk)) {
                                Log.w(TAG, "TTS playback stopped accepting audio; cancelling native synthesis.")
                                textToSpeechEngine.cancel()
                                break
                            }
                        }
                    } catch (error: Throwable) {
                        Log.e(TAG, "TTS playback consumer failed; cancelling native synthesis.", error)
                        playbackFailure.set(error)
                        textToSpeechEngine.cancel()
                        chunks?.cancel()
                    }
                }
            }

            val synthesisStartedNanos = System.nanoTime()
            val result = try {
                textToSpeechEngine.synthesize(
                    TextToSpeechRequest(
                        text = request.text,
                        languageCode = request.settings.languageCode,
                        voice = request.settings.voiceCondition,
                        speed = request.settings.speed,
                        sentenceSilenceScale = request.settings.sentenceSilenceScale,
                    ),
                ) { chunk ->
                    if (firstChunkNanos == null && chunk.isNotEmpty()) {
                        firstChunkNanos = System.nanoTime()
                        Log.i(TAG, "TTS first native audio chunk received: samples=${chunk.size}")
                    }
                    chunkCount += 1
                    streamedSampleCount += chunk.size
                    !cancelled.get() && (
                        chunks == null ||
                            chunks.trySendBlocking(chunk.copyOf()).isSuccess
                        )
                }
            } finally {
                chunks?.close()
            }
            val synthesisDurationMs = nanosToMillis(
                System.nanoTime() - synthesisStartedNanos,
            )
            consumer?.join()
            playbackFailure.get()?.let { throw it }
            check(!cancelled.get()) { "Speech synthesis was cancelled." }
            require(result.sampleRateHz == load.sampleRateHz) {
                "The voice model changed sample rate during synthesis."
            }
            val effectsStartedNanos = System.nanoTime()
            val outputSamples = audioEffectsProcessor.process(
                samples = result.samples,
                sampleRateHz = result.sampleRateHz,
                effects = request.settings.audioEffects,
                isCancelled = cancelled::get,
            )
            val effectsDurationMs = nanosToMillis(System.nanoTime() - effectsStartedNanos)
            if (effectsEnabled) {
                session = player.open(
                    sampleRateHz = load.sampleRateHz,
                    volume = request.settings.volume,
                    runAnchorNanos = runAnchorNanos,
                )
                Log.i(
                    TAG,
                    "TTS processed playback session opened: sampleRateHz=${load.sampleRateHz}, " +
                        "effectsMs=$effectsDurationMs",
                )
                writeFloatChunks(requireNotNull(session), outputSamples)
            }
            Log.i(
                TAG,
                "TTS native synthesis finished: durationMs=$synthesisDurationMs, chunks=$chunkCount, " +
                    "streamedSamples=$streamedSampleCount, resultSamples=${result.samples.size}, " +
                    "outputSamples=${outputSamples.size}, effectsMs=$effectsDurationMs, " +
                    "sampleRateHz=${result.sampleRateHz}",
            )
            val output = generatedAudioStore.saveLatest(outputSamples, result.sampleRateHz)
            Log.i(TAG, "TTS WAV retained: durationMs=${output.durationMs}, samples=${output.sampleCount}")
            emit(SpeechSynthesisEvent.Synthesized(output, synthesisDurationMs))
            val activeSession = requireNotNull(session)
            activeSession.awaitDrained()
            val playbackMetrics = activeSession.metrics()
            Log.i(
                TAG,
                "TTS playback drained: framesWritten=${playbackMetrics.framesWritten}, " +
                    "framesPresented=${playbackMetrics.framesPresented}, underruns=${playbackMetrics.underrunCount}, " +
                    "firstWriteMs=${playbackMetrics.firstWriteElapsedNanos?.let(::nanosToMillis)}, " +
                    "firstPresentationMs=${playbackMetrics.firstPresentationElapsedNanos?.let(::nanosToMillis)}",
            )
            player.release(completed = true)
            completed = true
            emit(
                SpeechSynthesisEvent.Completed(
                    output = output,
                    metrics = SpeechSynthesisMetrics(
                        timeToFirstChunkMs = firstChunkNanos?.let { nanosToMillis(it - runAnchorNanos) },
                        timeToFirstWriteMs = playbackMetrics.firstWriteElapsedNanos?.let(::nanosToMillis),
                        timeToFirstPresentationMs = playbackMetrics.firstPresentationElapsedNanos?.let(::nanosToMillis),
                        synthesisDurationMs = synthesisDurationMs,
                        generatedAudioDurationMs = output.durationMs,
                        realTimeFactor = output.durationMs.takeIf { it > 0 }
                            ?.let { synthesisDurationMs.toDouble() / it },
                        sampleRateHz = result.sampleRateHz,
                        playbackUnderrunCount = playbackMetrics.underrunCount,
                        loadDurationMs = load.loadDurationMs,
                        effectiveThreadCount = load.effectiveThreadCount,
                        conditioningDurationMs = result.stageMetrics.conditioningDurationMs,
                        tokenGenerationDurationMs = result.stageMetrics.tokenGenerationDurationMs,
                        decoderDurationMs = result.stageMetrics.decoderDurationMs,
                        generatedTokenCount = result.stageMetrics.generatedTokenCount,
                        conditioningCacheHit = result.stageMetrics.conditioningCacheHit,
                        peakProcessPssBytes = result.stageMetrics.peakProcessPssBytes,
                        availableDeviceMemoryBytes = result.stageMetrics.availableDeviceMemoryBytes,
                    ),
                ),
            )
        } catch (error: Throwable) {
            Log.e(TAG, "TTS synthesis flow failed: ${error.message}", error)
            throw error
        } finally {
            if (!completed) {
                generatedAudioStore.discardPartial()
                player.release(completed = false)
            }
            Log.i(TAG, "TTS synthesis cleanup complete: completed=$completed, cancelled=${cancelled.get()}")
        }
    }.flowOn(Dispatchers.Default)

    suspend fun replay(audio: GeneratedAudioFile, volume: Float) {
        require(volume in 0f..1f) { "Playback volume must be between 0 and 1." }
        cancelled.set(false)
        Log.i(
            TAG,
            "TTS replay requested: sampleRateHz=${audio.sampleRateHz}, samples=${audio.sampleCount}, " +
                "durationMs=${audio.durationMs}, volume=$volume",
        )
        val session = player.open(
            sampleRateHz = audio.sampleRateHz,
            volume = volume,
            runAnchorNanos = System.nanoTime(),
        )
        var completed = false
        try {
            generatedAudioStore.streamPcm16(audio) { chunk ->
                !cancelled.get() && session.writePcm16(chunk)
            }
            check(!cancelled.get()) { "Playback was cancelled." }
            session.awaitDrained()
            player.release(completed = true)
            completed = true
            Log.i(TAG, "TTS replay completed.")
        } catch (error: Throwable) {
            Log.e(TAG, "TTS replay failed: ${error.message}", error)
            throw error
        } finally {
            if (!completed) player.release(completed = false)
        }
    }

    fun pausePlayback() {
        Log.i(TAG, "TTS playback pause requested.")
        player.pause()
    }

    fun resumePlayback() {
        Log.i(TAG, "TTS playback resume requested.")
        player.resume()
    }

    fun cancel() {
        Log.i(TAG, "TTS cancellation requested.")
        cancelled.set(true)
        textToSpeechEngine.cancel()
        player.stop()
    }

    fun unloadRuntime() = textToSpeechEngine.unload()

    private fun nanosToMillis(nanos: Long): Long = nanos.coerceAtLeast(0) / 1_000_000L

    private fun writeFloatChunks(
        session: com.dmitriim.localaiplayground.core.audio.output.api.SpeechPlaybackSession,
        samples: FloatArray,
    ) {
        var offset = 0
        while (offset < samples.size) {
            check(!cancelled.get()) { "Speech synthesis was cancelled." }
            val end = minOf(offset + PROCESSED_AUDIO_CHUNK_SAMPLES, samples.size)
            check(session.write(samples.copyOfRange(offset, end))) {
                "Speech playback stopped accepting processed audio."
            }
            offset = end
        }
    }

    companion object {
        const val MAX_TEXT_CHARACTERS = 2_000
        private const val AUDIO_QUEUE_CAPACITY = 4
        private const val PROCESSED_AUDIO_CHUNK_SAMPLES = 4_096
        private const val TAG = "AiP123Tts"
    }
}
