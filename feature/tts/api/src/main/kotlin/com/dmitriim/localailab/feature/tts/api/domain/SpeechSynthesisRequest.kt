package com.dmitriim.localailab.feature.tts.api.domain

import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.tts.TextToSpeechVoiceCondition
import com.dmitriim.localailab.core.audio.processing.SpeechAudioEffects
import java.util.UUID

data class SpeechSynthesisRequest(
    val modelId: ModelId,
    val text: String,
    val settings: SpeechSynthesisSettings,
    val runId: String = UUID.randomUUID().toString(),
)

data class SpeechSynthesisSettings(
    val languageCode: String,
    val voiceCondition: TextToSpeechVoiceCondition,
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
        if (voiceCondition is TextToSpeechVoiceCondition.FixedSpeaker) {
            require(voiceCondition.speakerId >= 0) { "Speaker ID cannot be negative." }
        }
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
