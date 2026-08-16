package com.dmitriim.localailab.core.model.manifest

import kotlinx.serialization.Serializable

/** Stable, adapter-owned runtime profile identifier. Values remain extensible across app releases. */
@Serializable
@JvmInline
value class ModelProfileId(val value: String) {
    companion object {
        val LLM = ModelProfileId("LLM")
        val WHISPER_STT = ModelProfileId("WHISPER_STT")
        val PARAKEET_CTC_STT = ModelProfileId("PARAKEET_CTC_STT")
        val GIGAAM_CTC_STT = ModelProfileId("GIGAAM_CTC_STT")
        val ZIPFORMER_STT = ModelProfileId("ZIPFORMER_STT")
        val SENSE_VOICE_STT = ModelProfileId("SENSE_VOICE_STT")
        val PARAFORMER_STT = ModelProfileId("PARAFORMER_STT")
        val MOONSHINE_STT = ModelProfileId("MOONSHINE_STT")
        val MOONSHINE_V1_STT = ModelProfileId("MOONSHINE_V1_STT")
        val VOSK_STT = ModelProfileId("VOSK_STT")
        val ANDROID_SPEECH_RECOGNIZER_STT = ModelProfileId("ANDROID_SPEECH_RECOGNIZER_STT")
        val ANDROID_TEXT_TO_SPEECH_TTS = ModelProfileId("ANDROID_TEXT_TO_SPEECH_TTS")
        val SUPERTONIC_TTS = ModelProfileId("SUPERTONIC_TTS")
        val PIPER_VITS_TTS = ModelProfileId("PIPER_VITS_TTS")
        val KOKORO_TTS = ModelProfileId("KOKORO_TTS")
        val POCKET_TTS = ModelProfileId("POCKET_TTS")
        val CHATTERBOX_TURBO_Q4 = ModelProfileId("CHATTERBOX_TURBO_Q4")
    }
}

/** Existing persisted identifiers plus the Piper/VITS proof profile. */
object ModelProfileIds {
    val LLM = ModelProfileId("LLM")
    val WHISPER_STT = ModelProfileId("WHISPER_STT")
    val PARAKEET_CTC_STT = ModelProfileId("PARAKEET_CTC_STT")
    val GIGAAM_CTC_STT = ModelProfileId("GIGAAM_CTC_STT")
    val ZIPFORMER_STT = ModelProfileId("ZIPFORMER_STT")
    val SENSE_VOICE_STT = ModelProfileId("SENSE_VOICE_STT")
    val PARAFORMER_STT = ModelProfileId("PARAFORMER_STT")
    val MOONSHINE_STT = ModelProfileId("MOONSHINE_STT")
    val MOONSHINE_V1_STT = ModelProfileId("MOONSHINE_V1_STT")
    val VOSK_STT = ModelProfileId("VOSK_STT")
    val ANDROID_SPEECH_RECOGNIZER_STT = ModelProfileId("ANDROID_SPEECH_RECOGNIZER_STT")
    val ANDROID_TEXT_TO_SPEECH_TTS = ModelProfileId("ANDROID_TEXT_TO_SPEECH_TTS")
    val SUPERTONIC_TTS = ModelProfileId("SUPERTONIC_TTS")
    val PIPER_VITS_TTS = ModelProfileId("PIPER_VITS_TTS")
    val KOKORO_TTS = ModelProfileId("KOKORO_TTS")
    val POCKET_TTS = ModelProfileId("POCKET_TTS")
    val CHATTERBOX_TURBO_Q4 = ModelProfileId("CHATTERBOX_TURBO_Q4")
}
