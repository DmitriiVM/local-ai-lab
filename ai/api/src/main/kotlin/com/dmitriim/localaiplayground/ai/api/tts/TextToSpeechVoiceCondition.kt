package com.dmitriim.localaiplayground.ai.api.tts

sealed interface TextToSpeechVoiceCondition {
    data class FixedSpeaker(val speakerId: Int) : TextToSpeechVoiceCondition

    data class PlatformVoice(val voiceId: String) : TextToSpeechVoiceCondition

    /** App-private, mono PCM16 reference. External document URIs never cross this boundary. */
    data class ReferenceAudio(
        val referenceId: String,
        val displayName: String,
        val pcmFilePath: String,
        val sampleRateHz: Int,
    ) : TextToSpeechVoiceCondition
}
