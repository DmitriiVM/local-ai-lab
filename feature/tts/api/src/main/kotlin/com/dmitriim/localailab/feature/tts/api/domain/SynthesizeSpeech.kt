package com.dmitriim.localailab.feature.tts.api.domain

import com.dmitriim.localailab.core.audio.output.model.GeneratedAudioFile
import kotlinx.coroutines.flow.Flow

/** Public TTS synthesis operation contract for consumers outside the TTS feature. */
interface SynthesizeSpeech {
    fun execute(request: SpeechSynthesisRequest): Flow<SpeechSynthesisEvent>

    suspend fun replay(audio: GeneratedAudioFile, volume: Float)

    fun pausePlayback()

    fun resumePlayback()

    fun cancel()

    fun unloadRuntime()

    companion object {
        const val MAX_TEXT_CHARACTERS = 2_000
    }
}
