package com.dmitriim.localaiplayground.core.audio.output.api

import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackMetrics

interface SpeechPlaybackSession {
    val sampleRateHz: Int

    fun write(samples: FloatArray): Boolean

    fun writePcm16(pcm16: ByteArray): Boolean

    suspend fun awaitDrained()

    fun metrics(): SpeechPlaybackMetrics
}
