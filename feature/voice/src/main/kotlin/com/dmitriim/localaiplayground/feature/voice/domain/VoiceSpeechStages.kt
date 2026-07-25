package com.dmitriim.localaiplayground.feature.voice.domain

import com.dmitriim.localaiplayground.ai.api.SpeechToTextEngine
import com.dmitriim.localaiplayground.ai.api.SpeechToTextLoadRequest
import com.dmitriim.localaiplayground.ai.api.SpeechToTextRequest
import com.dmitriim.localaiplayground.ai.api.TextToSpeechEngine
import com.dmitriim.localaiplayground.ai.api.TextToSpeechLoadRequest
import com.dmitriim.localaiplayground.ai.api.TextToSpeechRequest
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
    engine.load(
        SpeechToTextLoadRequest(
            modelDirectory = model.modelDirectory,
            languageCode = languageCode,
            threadCount = threadCount,
        ),
    )
    return try {
        val transcript = StringBuilder()
        var processingDurationMs = 0L
        audioInputStore.forEachSegment(input) { samples ->
            ensureNotCancelled()
            val result = engine.transcribe(SpeechToTextRequest(samples, input.sampleRateHz))
            processingDurationMs += result.processingDurationMs
            if (result.text.isNotBlank()) {
                if (transcript.isNotEmpty()) transcript.append(' ')
                transcript.append(result.text.trim())
            }
        }
        TranscribedVoice(transcript.toString(), processingDurationMs)
    } finally {
        engine.unload()
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
    val load = engine.load(TextToSpeechLoadRequest(model.modelDirectory, request.ttsThreadCount))
    require(request.speakerId < load.speakerCount) {
        "Speaker ${request.speakerId} is unavailable for ${model.displayName}."
    }
    val session = player.open(load.sampleRateHz, request.volume, anchorNanos)
    onPlaybackOpened()
    var completed = false
    var firstChunkNanos: Long? = null
    val synthesisStartedNanos = System.nanoTime()
    try {
        engine.synthesize(
            TextToSpeechRequest(
                text = text,
                languageCode = request.languageCode,
                speakerId = request.speakerId,
                speed = request.speechRate,
                sentenceSilenceScale = 1f,
            ),
        ) { chunk ->
            if (firstChunkNanos == null && chunk.isNotEmpty()) firstChunkNanos = System.nanoTime()
            !isCancelled() && session.write(chunk)
        }
        ensureNotCancelled()
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
        )
    } finally {
        engine.unload()
        if (!completed) player.release(completed = false)
    }
}

private fun elapsedMs(startNanos: Long, endNanos: Long): Long =
    (endNanos - startNanos).coerceAtLeast(0) / 1_000_000L

private fun nanosToMs(nanos: Long): Long = nanos.coerceAtLeast(0) / 1_000_000L
