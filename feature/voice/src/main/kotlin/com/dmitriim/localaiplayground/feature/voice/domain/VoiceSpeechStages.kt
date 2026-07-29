package com.dmitriim.localaiplayground.feature.voice.domain

import android.util.Log
import com.dmitriim.localaiplayground.ai.api.SpeechToTextEngine
import com.dmitriim.localaiplayground.ai.api.SpeechToTextLoadRequest
import com.dmitriim.localaiplayground.ai.api.SpeechToTextRequest
import com.dmitriim.localaiplayground.ai.api.TextToSpeechEngine
import com.dmitriim.localaiplayground.ai.api.TextToSpeechLoadRequest
import com.dmitriim.localaiplayground.ai.api.TextToSpeechRequest
import com.dmitriim.localaiplayground.ai.api.TextToSpeechVoiceCondition
import com.dmitriim.localaiplayground.core.audio.input.model.PcmAudioInput
import com.dmitriim.localaiplayground.core.audio.input.storage.AudioInputStore
import com.dmitriim.localaiplayground.core.audio.output.api.StreamingSpeechPlayer
import com.dmitriim.localaiplayground.core.model.SpeechToTextModelReference
import com.dmitriim.localaiplayground.core.model.TextToSpeechModelReference

internal suspend fun transcribeVoiceInput(
    audioInputStore: AudioInputStore,
    engine: SpeechToTextEngine,
    input: PcmAudioInput,
    model: SpeechToTextModelReference,
    languageCode: String,
    threadCount: Int,
    ensureNotCancelled: () -> Unit,
): TranscribedVoice {
    Log.i(TAG, "Voice STT stage loading: model=${model.displayName}, language=$languageCode, requestedThreads=$threadCount")
    val load = engine.load(
        SpeechToTextLoadRequest(
            profileType = model.profileType,
            modelDirectory = model.modelDirectory,
            languageCode = languageCode,
            threadCount = threadCount,
        ),
    )
    Log.i(TAG, "Voice STT stage loaded: coldStart=${load.coldStart}, loadMs=${load.loadDurationMs}, effectiveThreads=${load.effectiveThreadCount}")
    return try {
        val transcript = StringBuilder()
        var processingDurationMs = 0L
        var segmentCount = 0
        audioInputStore.forEachSegment(input) { samples ->
            ensureNotCancelled()
            val segmentNumber = segmentCount + 1
            Log.i(TAG, "Voice STT segment started: number=$segmentNumber, samples=${samples.size}")
            val result = engine.transcribe(SpeechToTextRequest(samples, input.sampleRateHz))
            processingDurationMs += result.processingDurationMs
            segmentCount++
            Log.i(TAG, "Voice STT segment completed: number=$segmentNumber, processingMs=${result.processingDurationMs}, transcriptLength=${result.text.length}")
            if (result.text.isNotBlank()) {
                if (transcript.isNotEmpty()) transcript.append(' ')
                transcript.append(result.text.trim())
            }
        }
        TranscribedVoice(transcript.toString(), processingDurationMs).also {
            Log.i(TAG, "Voice STT stage completed: segments=$segmentCount, transcriptLength=${it.text.length}, processingMs=$processingDurationMs")
        }
    } catch (error: Throwable) {
        Log.e(TAG, "Voice STT stage failed: ${error.message}", error)
        throw error
    } finally {
        engine.unload()
        Log.i(TAG, "Voice STT stage unloaded.")
    }
}

internal suspend fun synthesizeAndPlayVoiceResponse(
    engine: TextToSpeechEngine,
    player: StreamingSpeechPlayer,
    text: String,
    request: VoiceTurnRequest,
    model: TextToSpeechModelReference,
    anchorNanos: Long,
    isCancelled: () -> Boolean,
    ensureNotCancelled: () -> Unit,
    onPlaybackOpened: () -> Unit,
): VoiceSpeechMetrics {
    require(text.isNotBlank()) { "The local model returned an empty response." }
    Log.i(TAG, "Voice TTS stage loading: model=${model.displayName}, textLength=${text.length}, requestedThreads=${request.ttsThreadCount}, speaker=${request.speakerId}, speed=${request.speechRate}, volume=${request.volume}")
    val load = engine.load(
        TextToSpeechLoadRequest(
            engineId = model.engineId,
            profileType = model.profileType,
            modelDirectory = model.modelDirectory,
            threadCount = request.ttsThreadCount,
        ),
    )
    Log.i(TAG, "Voice TTS stage loaded: coldStart=${load.coldStart}, loadMs=${load.loadDurationMs}, sampleRateHz=${load.sampleRateHz}, speakers=${load.speakerCount}, effectiveThreads=${load.effectiveThreadCount}")
    require(load.speakerCount?.let { request.speakerId < it } == true) {
        "Speaker ${request.speakerId} is unavailable for ${model.displayName}."
    }
    val session = player.open(load.sampleRateHz, request.volume, anchorNanos)
    onPlaybackOpened()
    Log.i(TAG, "Voice TTS playback session opened: sampleRateHz=${load.sampleRateHz}")
    var completed = false
    var firstChunkNanos: Long? = null
    var chunkCount = 0
    var streamedSamples = 0L
    val synthesisStartedNanos = System.nanoTime()
    try {
        engine.synthesize(
            TextToSpeechRequest(
                text = text,
                languageCode = request.languageCode,
                voice = TextToSpeechVoiceCondition.FixedSpeaker(request.speakerId),
                speed = request.speechRate,
                sentenceSilenceScale = 1f,
            ),
        ) { chunk ->
            if (firstChunkNanos == null && chunk.isNotEmpty()) {
                firstChunkNanos = System.nanoTime()
                Log.i(TAG, "Voice TTS first native audio chunk received: samples=${chunk.size}")
            }
            chunkCount++
            streamedSamples += chunk.size
            !isCancelled() && session.write(chunk)
        }
        ensureNotCancelled()
        Log.i(TAG, "Voice TTS native synthesis completed: chunks=$chunkCount, streamedSamples=$streamedSamples")
        session.awaitDrained()
        ensureNotCancelled()
        val playback = session.metrics()
        player.release(completed = true)
        completed = true
        return VoiceSpeechMetrics(
            timeToFirstChunkMs = firstChunkNanos?.let { elapsedMs(anchorNanos, it) },
            timeToFirstWriteMs = playback.firstWriteElapsedNanos?.let(::nanosToMs),
            timeToFirstPresentationMs = playback.firstPresentationElapsedNanos?.let(::nanosToMs),
            completionDurationMs = elapsedMs(synthesisStartedNanos, System.nanoTime()),
        ).also { metrics ->
            Log.i(TAG, "Voice TTS playback completed: framesWritten=${playback.framesWritten}, framesPresented=${playback.framesPresented}, underruns=${playback.underrunCount}, completionMs=${metrics.completionDurationMs}")
        }
    } catch (error: Throwable) {
        Log.e(TAG, "Voice TTS stage failed: ${error.message}", error)
        throw error
    } finally {
        engine.unload()
        if (!completed) player.release(completed = false)
        Log.i(TAG, "Voice TTS stage cleanup completed: completed=$completed")
    }
}

private fun elapsedMs(startNanos: Long, endNanos: Long): Long =
    (endNanos - startNanos).coerceAtLeast(0) / 1_000_000L

private fun nanosToMs(nanos: Long): Long = nanos.coerceAtLeast(0) / 1_000_000L

private const val TAG = "AiP123Voice"
