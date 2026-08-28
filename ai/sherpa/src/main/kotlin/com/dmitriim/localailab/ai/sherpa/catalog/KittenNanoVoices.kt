package com.dmitriim.localailab.ai.sherpa.catalog

/** Speaker order in the pinned sherpa-onnx KittenTTS Nano v0.1 export. */
internal fun kittenNanoV01Voices() = listOf(
    kittenVoice("expr-voice-2-m", 0, "Male"),
    kittenVoice("expr-voice-2-f", 1, "Female"),
    kittenVoice("expr-voice-3-m", 2, "Male"),
    kittenVoice("expr-voice-3-f", 3, "Female"),
    kittenVoice("expr-voice-4-m", 4, "Male"),
    kittenVoice("expr-voice-4-f", 5, "Female"),
    kittenVoice("expr-voice-5-m", 6, "Male"),
    kittenVoice("expr-voice-5-f", 7, "Female"),
)

private fun kittenVoice(id: String, speakerId: Int, gender: String) = ttsVoice(
    id = id,
    displayName = id,
    speakerId = speakerId,
    description = "$gender · English",
    languages = arrayOf("en"),
)
