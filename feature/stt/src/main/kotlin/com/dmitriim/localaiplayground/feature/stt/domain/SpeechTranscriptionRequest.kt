package com.dmitriim.localaiplayground.feature.stt.domain

import com.dmitriim.localaiplayground.core.audio.input.model.PcmAudioInput
import com.dmitriim.localaiplayground.core.model.manifest.ModelId

data class SpeechTranscriptionRequest(
    val modelId: ModelId,
    val input: PcmAudioInput,
    val settings: SttTranscriptionSettings,
)

data class SttTranscriptionSettings(
    val languageCode: String,
    val threadCount: String,
) {
    fun toEffective(): EffectiveSttTranscriptionSettings {
        val threads = threadCount.toIntOrNull() ?: error("Thread count must be a whole number.")
        require(threads in 0..64) { "Thread count must be between 0 and 64." }
        return EffectiveSttTranscriptionSettings(languageCode, threads)
    }
}

data class EffectiveSttTranscriptionSettings(val languageCode: String, val threadCount: Int)
