package com.dmitriim.localaiplayground.core.audio.output.api

import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackState
import kotlinx.coroutines.flow.StateFlow

interface StreamingSpeechPlayer {
    val state: StateFlow<SpeechPlaybackState>

    fun open(sampleRateHz: Int, volume: Float, runAnchorNanos: Long): SpeechPlaybackSession

    fun pause()

    fun resume()

    fun stop()

    fun release(completed: Boolean)
}
