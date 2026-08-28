package com.dmitriim.localailab.ai.api.tts

/** Describes the voice-selection mechanism requested from a text-to-speech runtime. */
sealed interface TextToSpeechVoiceCondition {
    /** Selects one model-defined speaker. */
    data class FixedSpeaker(val speakerId: Int) : TextToSpeechVoiceCondition

    /** Selects a voice exposed by the operating system's text-to-speech service. */
    data class PlatformVoice(val voiceId: String) : TextToSpeechVoiceCondition

    /** App-private, mono PCM16 reference. External document URIs never cross this boundary. */
    data class ReferenceAudio(
        val referenceId: String,
        val displayName: String,
        val pcmFilePath: String,
        val sampleRateHz: Int,
    ) : TextToSpeechVoiceCondition
}
