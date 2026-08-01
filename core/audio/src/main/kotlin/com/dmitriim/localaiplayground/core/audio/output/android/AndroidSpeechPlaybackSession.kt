package com.dmitriim.localaiplayground.core.audio.output.android

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.os.SystemClock
import android.util.Log
import com.dmitriim.localaiplayground.core.audio.output.api.SpeechPlaybackSession
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackMetrics
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackState
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackStatus
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.roundToInt

internal class AndroidSpeechPlaybackSession(
    private val audioManager: AudioManager,
    override val sampleRateHz: Int,
    volume: Float,
    private val runAnchorNanos: Long,
    private val currentState: () -> SpeechPlaybackState,
    private val updateState: (SpeechPlaybackState) -> Unit,
    private val isActive: () -> Boolean,
) : SpeechPlaybackSession {
    private val closed = AtomicBoolean(false)
    private val stopped = AtomicBoolean(false)
    @Volatile private var paused = false
    @Volatile private var pausedByFocus = false
    private var started = false
    private var playbackStarted = false
    private var focusGranted = false
    private var framePositionAtStart = 0L
    private var underrunsAtStart = 0
    private var framesWritten = 0L
    private var firstWriteNanos: Long? = null
    private var firstPresentationNanos: Long? = null
    private var lastPresentedFrames = 0L
    private var lastProgressNanos = SystemClock.elapsedRealtimeNanos()
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener(::onAudioFocusChange)
        .build()
    private val track: AudioTrack

    init {
        require(sampleRateHz > 0) { "Invalid speech sample rate: $sampleRateHz." }
        require(volume in 0f..1f) { "Playback volume must be between 0 and 1." }
        val minimum = AudioTrack.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        require(minimum > 0) { "Android rejected $sampleRateHz Hz PCM playback." }
        val capacity160Ms = sampleRateHz * PCM_BYTES_PER_FRAME * 160 / 1_000
        track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRateHz)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .setBufferSizeInBytes(max(minimum, capacity160Ms))
            .build()
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            error("Android could not initialize speech playback at $sampleRateHz Hz.")
        }
        track.setVolume(volume)
        Log.i(TAG, "AudioTrack initialized: sampleRateHz=$sampleRateHz, minBufferBytes=$minimum, bufferBytes=${max(minimum, capacity160Ms)}")
    }

    override fun write(samples: FloatArray): Boolean {
        if (samples.isEmpty()) return !stopped.get()
        val pcm = ByteArray(samples.size * PCM_BYTES_PER_FRAME)
        samples.forEachIndexed { index, sample ->
            val value = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE)
                .roundToInt()
                .toShort()
                .toInt()
            pcm[index * 2] = value.toByte()
            pcm[index * 2 + 1] = (value shr 8).toByte()
        }
        return writePcm16(pcm)
    }

    override fun writePcm16(pcm16: ByteArray): Boolean {
        require(pcm16.size % PCM_BYTES_PER_FRAME == 0) {
            "PCM16 audio must contain complete frames."
        }
        if (pcm16.isEmpty()) return !stopped.get()
        if (!started) prepareStart()
        var offset = 0
        while (offset < pcm16.size && !stopped.get() && !closed.get()) {
            if (paused) {
                Thread.sleep(PAUSE_POLL_MS)
                continue
            }
            val count = track.write(
                pcm16,
                offset,
                pcm16.size - offset,
                AudioTrack.WRITE_NON_BLOCKING,
            )
            when {
                count > 0 -> {
                    if (firstWriteNanos == null) firstWriteNanos = System.nanoTime()
                    offset += count
                    framesWritten += count / PCM_BYTES_PER_FRAME
                    if (!playbackStarted) {
                        track.play()
                        playbackStarted = true
                        Log.i(TAG, "AudioTrack playback started: firstWriteBytes=$count")
                        refresh(status = SpeechPlaybackStatus.PLAYING)
                    } else {
                        refresh()
                    }
                }
                count == 0 -> Thread.sleep(WRITE_POLL_MS)
                else -> {
                    Log.e(TAG, "AudioTrack write failed: result=$count, offset=$offset, totalBytes=${pcm16.size}")
                    error("Android speech playback failed while writing PCM: $count.")
                }
            }
        }
        return offset == pcm16.size && !stopped.get() && !closed.get()
    }

    override suspend fun awaitDrained() {
        if (!playbackStarted || stopped.get()) return
        Log.i(TAG, "Waiting for AudioTrack drain: framesWritten=$framesWritten")
        while (!stopped.get() && !closed.get()) {
            refresh()
            if (!paused && presentedFrames() >= framesWritten) {
                Log.i(TAG, "AudioTrack drained: framesPresented=${presentedFrames()}, underruns=${track.underrunCount - underrunsAtStart}")
                return
            }
            if (
                !paused &&
                SystemClock.elapsedRealtimeNanos() - lastProgressNanos > STALL_TIMEOUT_NANOS
            ) {
                Log.e(TAG, "AudioTrack drain stalled: framesWritten=$framesWritten, framesPresented=${presentedFrames()}, paused=$paused")
                error("Android speech playback stopped making progress.")
            }
            delay(DRAIN_POLL_MS)
        }
    }

    override fun metrics(): SpeechPlaybackMetrics {
        refresh()
        return SpeechPlaybackMetrics(
            firstWriteElapsedNanos = firstWriteNanos?.minus(runAnchorNanos),
            firstPresentationElapsedNanos = firstPresentationNanos?.minus(runAnchorNanos),
            framesWritten = framesWritten,
            framesPresented = presentedFrames(),
            underrunCount = (track.underrunCount - underrunsAtStart).coerceAtLeast(0),
        )
    }

    fun pause(pausedForFocus: Boolean) {
        if (!playbackStarted || stopped.get() || paused) return
        paused = true
        pausedByFocus = pausedForFocus
        track.pause()
        Log.i(TAG, "AudioTrack paused: byFocus=$pausedForFocus")
        refresh(
            status = SpeechPlaybackStatus.PAUSED,
            focusMessage = if (pausedForFocus) {
                "Playback paused after transient audio focus loss."
            } else {
                null
            },
        )
    }

    fun resume(fromFocus: Boolean) {
        if (!playbackStarted || stopped.get() || !paused) return
        if (fromFocus && !pausedByFocus) return
        paused = false
        pausedByFocus = false
        lastProgressNanos = SystemClock.elapsedRealtimeNanos()
        track.play()
        Log.i(TAG, "AudioTrack resumed: fromFocus=$fromFocus")
        refresh(status = SpeechPlaybackStatus.PLAYING, focusMessage = null)
    }

    fun stop(message: String?) {
        if (!stopped.compareAndSet(false, true)) return
        try {
            track.pause()
            track.flush()
        } catch (_: IllegalStateException) {
        }
        refresh(status = SpeechPlaybackStatus.STOPPED, focusMessage = message)
        Log.i(TAG, "AudioTrack stopped: message=$message")
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        stopped.set(true)
        try {
            if (track.playState != AudioTrack.PLAYSTATE_STOPPED) track.stop()
        } catch (_: IllegalStateException) {
        }
        track.release()
        if (focusGranted) {
            audioManager.abandonAudioFocusRequest(focusRequest)
            focusGranted = false
        }
        Log.i(TAG, "AudioTrack released.")
    }

    private fun prepareStart() {
        val focusResult = audioManager.requestAudioFocus(focusRequest)
        Log.i(TAG, "Audio focus requested: result=$focusResult")
        require(focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            "Another app currently owns audio playback."
        }
        focusGranted = true
        framePositionAtStart = track.playbackHeadPosition.toLong() and UINT32_MASK
        underrunsAtStart = track.underrunCount
        started = true
        lastProgressNanos = SystemClock.elapsedRealtimeNanos()
    }

    private fun onAudioFocusChange(change: Int) {
        if (!isActive()) return
        Log.i(TAG, "Audio focus changed: change=$change")
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> resume(fromFocus = true)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
            -> pause(pausedForFocus = true)
            AudioManager.AUDIOFOCUS_LOSS -> {
                stop("Playback stopped after permanent audio focus loss.")
            }
        }
    }

    private fun refresh(
        status: SpeechPlaybackStatus? = null,
        focusMessage: String? = currentState().focusMessage,
    ) {
        pollFirstPresentation()
        val presented = presentedFrames()
        if (presented > lastPresentedFrames) {
            lastPresentedFrames = presented
            lastProgressNanos = SystemClock.elapsedRealtimeNanos()
        }
        updateState(
            SpeechPlaybackState(
                status = status ?: currentState().status,
                positionMs = presented * 1_000L / sampleRateHz,
                queuedDurationMs = framesWritten * 1_000L / sampleRateHz,
                focusMessage = focusMessage,
            ),
        )
    }

    private fun pollFirstPresentation() {
        if (firstPresentationNanos != null || !playbackStarted) return
        val timestamp = AudioTimestamp()
        if (track.getTimestamp(timestamp) && timestamp.framePosition > framePositionAtStart) {
            val presentedFrames = timestamp.framePosition - framePositionAtStart
            val elapsedForFrames = presentedFrames * NANOS_PER_SECOND / sampleRateHz
            firstPresentationNanos = timestamp.nanoTime - elapsedForFrames
            Log.i(TAG, "AudioTrack first presentation timestamp received: framePosition=${timestamp.framePosition}")
        }
    }

    private fun presentedFrames(): Long {
        if (!playbackStarted) return 0
        val position = track.playbackHeadPosition.toLong() and UINT32_MASK
        return ((position - framePositionAtStart) and UINT32_MASK).coerceAtMost(framesWritten)
    }

    private companion object {
        const val TAG = "AiP123Tts"
        const val PCM_BYTES_PER_FRAME = 2
        const val WRITE_POLL_MS = 4L
        const val PAUSE_POLL_MS = 10L
        const val DRAIN_POLL_MS = 10L
        const val NANOS_PER_SECOND = 1_000_000_000L
        const val STALL_TIMEOUT_NANOS = 5L * NANOS_PER_SECOND
        const val UINT32_MASK = 0xffff_ffffL
    }
}
