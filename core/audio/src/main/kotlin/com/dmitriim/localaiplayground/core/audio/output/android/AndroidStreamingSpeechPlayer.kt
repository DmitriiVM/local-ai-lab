package com.dmitriim.localaiplayground.core.audio.output.android

import android.app.Application
import android.content.Context
import android.media.AudioManager
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.audio.output.api.SpeechPlaybackSession
import com.dmitriim.localaiplayground.core.audio.output.api.StreamingSpeechPlayer
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackState
import com.dmitriim.localaiplayground.core.audio.output.model.SpeechPlaybackStatus
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AndroidStreamingSpeechPlayer(
    application: Application,
) : StreamingSpeechPlayer {
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mutableState = MutableStateFlow(SpeechPlaybackState())
    override val state: StateFlow<SpeechPlaybackState> = mutableState.asStateFlow()
    private val lock = Any()
    private var active: AndroidSpeechPlaybackSession? = null

    override fun open(
        sampleRateHz: Int,
        volume: Float,
        runAnchorNanos: Long,
    ): SpeechPlaybackSession = synchronized(lock) {
        releaseLocked(completed = false, updateState = false)
        lateinit var session: AndroidSpeechPlaybackSession
        session = AndroidSpeechPlaybackSession(
            audioManager = audioManager,
            sampleRateHz = sampleRateHz,
            volume = volume,
            runAnchorNanos = runAnchorNanos,
            currentState = { mutableState.value },
            updateState = { mutableState.value = it },
            isActive = { synchronized(lock) { active === session } },
        )
        active = session
        mutableState.value = SpeechPlaybackState(status = SpeechPlaybackStatus.READY)
        session
    }

    override fun pause() {
        synchronized(lock) { active?.pause(pausedForFocus = false) }
    }

    override fun resume() {
        synchronized(lock) { active?.resume(fromFocus = false) }
    }

    override fun stop() {
        synchronized(lock) {
            active?.stop("Playback stopped.")
            releaseLocked(completed = false, updateState = true)
        }
    }

    override fun release(completed: Boolean) {
        synchronized(lock) { releaseLocked(completed, updateState = true) }
    }

    private fun releaseLocked(completed: Boolean, updateState: Boolean) {
        val session = active
        active = null
        session?.close()
        if (updateState) {
            mutableState.value = mutableState.value.copy(
                status = if (completed) SpeechPlaybackStatus.COMPLETED else SpeechPlaybackStatus.STOPPED,
            )
        }
    }
}
