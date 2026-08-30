package com.dmitriim.localailab.feature.stt.api.domain

import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.core.audio.input.model.PcmAudioInput
import java.util.UUID

data class SpeechTranscriptionRequest(
    val modelId: ModelId,
    val input: PcmAudioInput,
    val settings: SttTranscriptionSettings,
    val runId: String = UUID.randomUUID().toString(),
    val keepLoaded: Boolean = false,
    val extendedProfiling: Boolean = false,
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
