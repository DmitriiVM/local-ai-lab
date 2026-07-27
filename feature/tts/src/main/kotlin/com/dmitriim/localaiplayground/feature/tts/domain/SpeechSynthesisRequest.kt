package com.dmitriim.localaiplayground.feature.tts.domain

import com.dmitriim.localaiplayground.core.audio.processing.SpeechAudioEffects
import com.dmitriim.localaiplayground.core.model.ModelId

data class SpeechSynthesisRequest(
    val modelId: ModelId,
    val text: String,
    val settings: SpeechSynthesisSettings,
)

data class SpeechSynthesisSettings(
    val languageCode: String,
    val speakerId: Int,
    val voiceName: String? = null,
    val expectedSpeakerCount: Int? = null,
    val speed: Float,
    val sentenceSilenceScale: Float,
    val volume: Float,
    val threadCount: Int,
    val audioEffects: SpeechAudioEffects = SpeechAudioEffects(),
) {
    fun validate() {
        require(languageCode in setOf("en", "ru", "zh")) { "Select a supported language." }
        require(speakerId >= 0) { "Speaker ID cannot be negative." }
        require(expectedSpeakerCount == null || expectedSpeakerCount > 0) {
            "Voice metadata must declare at least one speaker."
        }
        require(speed in 0.5f..2f) { "Speech rate must be between 0.5 and 2.0." }
        require(sentenceSilenceScale in 0f..2f) {
            "Sentence silence must be between 0.0 and 2.0."
        }
        require(volume in 0f..1f) { "Playback volume must be between 0 and 1." }
        require(threadCount in 0..64) { "Thread count must be between 0 and 64." }
        audioEffects.validate()
    }
}
