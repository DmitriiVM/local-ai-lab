package com.dmitriim.localailab.feature.tts.api.domain

/** Public TTS preview operation contract for consumers outside the TTS feature. */
interface PreviewSpeech {
    suspend fun execute(request: SpeechPreviewRequest)

    fun cancel()
}
