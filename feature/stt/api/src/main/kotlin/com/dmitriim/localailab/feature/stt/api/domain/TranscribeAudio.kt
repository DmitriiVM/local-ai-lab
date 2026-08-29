package com.dmitriim.localailab.feature.stt.api.domain

import kotlinx.coroutines.flow.Flow

/** Public STT operation contract for consumers outside the STT feature. */
interface TranscribeAudio {
    fun execute(request: SpeechTranscriptionRequest): Flow<SpeechTranscriptionEvent>

    fun cancel()

    fun unload()
}
