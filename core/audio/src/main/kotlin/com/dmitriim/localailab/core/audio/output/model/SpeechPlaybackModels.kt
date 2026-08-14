package com.dmitriim.localailab.core.audio.output.model

enum class SpeechPlaybackStatus {
    IDLE,
    READY,
    PLAYING,
    PAUSED,
    COMPLETED,
    STOPPED,
}

data class SpeechPlaybackState(
    val status: SpeechPlaybackStatus = SpeechPlaybackStatus.IDLE,
    val positionMs: Long = 0,
    val queuedDurationMs: Long = 0,
    val focusMessage: String? = null,
)

data class SpeechPlaybackMetrics(
    val firstWriteElapsedNanos: Long?,
    val firstPresentationElapsedNanos: Long?,
    val framesWritten: Long,
    val framesPresented: Long,
    val underrunCount: Int,
)
