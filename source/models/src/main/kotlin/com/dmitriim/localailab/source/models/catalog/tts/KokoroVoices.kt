package com.dmitriim.localailab.source.models.catalog.tts

/**
 * Official speaker order for sherpa-onnx kokoro-multi-lang-v1_0.
 * https://k2-fsa.github.io/sherpa/onnx/tts/pretrained_models/kokoro.html
 */
internal fun kokoroV1Voices() = listOf(
    kokoroVoice("af_alloy", 0, "en"),
    kokoroVoice("af_aoede", 1, "en"),
    kokoroVoice("af_bella", 2, "en"),
    kokoroVoice("af_heart", 3, "en"),
    kokoroVoice("af_jessica", 4, "en"),
    kokoroVoice("af_kore", 5, "en"),
    kokoroVoice("af_nicole", 6, "en"),
    kokoroVoice("af_nova", 7, "en"),
    kokoroVoice("af_river", 8, "en"),
    kokoroVoice("af_sarah", 9, "en"),
    kokoroVoice("af_sky", 10, "en"),
    kokoroVoice("am_adam", 11, "en"),
    kokoroVoice("am_echo", 12, "en"),
    kokoroVoice("am_eric", 13, "en"),
    kokoroVoice("am_fenrir", 14, "en"),
    kokoroVoice("am_liam", 15, "en"),
    kokoroVoice("am_michael", 16, "en"),
    kokoroVoice("am_onyx", 17, "en"),
    kokoroVoice("am_puck", 18, "en"),
    kokoroVoice("am_santa", 19, "en"),
    kokoroVoice("bf_alice", 20, "en"),
    kokoroVoice("bf_emma", 21, "en"),
    kokoroVoice("bf_isabella", 22, "en"),
    kokoroVoice("bf_lily", 23, "en"),
    kokoroVoice("bm_daniel", 24, "en"),
    kokoroVoice("bm_fable", 25, "en"),
    kokoroVoice("bm_george", 26, "en"),
    kokoroVoice("bm_lewis", 27, "en"),
    kokoroVoice("ef_dora", 28, "es"),
    kokoroVoice("em_alex", 29, "es"),
    kokoroVoice("ff_siwis", 30, "fr"),
    kokoroVoice("hf_alpha", 31, "hi"),
    kokoroVoice("hf_beta", 32, "hi"),
    kokoroVoice("hm_omega", 33, "hi"),
    kokoroVoice("hm_psi", 34, "hi"),
    kokoroVoice("if_sara", 35, "it"),
    kokoroVoice("im_nicola", 36, "it"),
    kokoroVoice("jf_alpha", 37, "ja"),
    kokoroVoice("jf_gongitsune", 38, "ja"),
    kokoroVoice("jf_nezumi", 39, "ja"),
    kokoroVoice("jf_tebukuro", 40, "ja"),
    kokoroVoice("jm_kumo", 41, "ja"),
    kokoroVoice("pf_dora", 42, "pt"),
    kokoroVoice("pm_alex", 43, "pt"),
    kokoroVoice("pm_santa", 44, "pt"),
    kokoroVoice("zf_xiaobei", 45, "zh"),
    kokoroVoice("zf_xiaoni", 46, "zh"),
    kokoroVoice("zf_xiaoxiao", 47, "zh"),
    kokoroVoice("zf_xiaoyi", 48, "zh"),
    kokoroVoice("zm_yunjian", 49, "zh"),
    kokoroVoice("zm_yunxi", 50, "zh"),
    kokoroVoice("zm_yunxia", 51, "zh"),
    kokoroVoice("zm_yunyang", 52, "zh"),
)

private fun kokoroVoice(id: String, speakerId: Int, language: String) = ttsVoice(
    id = id,
    displayName = id.substringAfter('_').replaceFirstChar(Char::uppercase),
    speakerId = speakerId,
    description = kokoroVoiceDescription(id),
    languages = arrayOf(language),
)

private fun kokoroVoiceDescription(id: String): String {
    val gender = when (id.getOrNull(1)) {
        'f' -> "Female"
        'm' -> "Male"
        else -> error("Unknown Kokoro voice gender: $id")
    }
    val language = when (id.firstOrNull()) {
        'a' -> "American English"
        'b' -> "British English"
        'e' -> "Spanish"
        'f' -> "French"
        'h' -> "Hindi"
        'i' -> "Italian"
        'j' -> "Japanese"
        'p' -> "Brazilian Portuguese"
        'z' -> "Mandarin Chinese"
        else -> error("Unknown Kokoro voice language: $id")
    }
    val quality = kokoroOverallQuality(id)
    return listOfNotNull(gender, language, quality?.let { "Quality $it" }).joinToString(" · ")
}

/**
 * Overall grades published with Kokoro's voice metadata; some voices have no published grade.
 * https://huggingface.co/hexgrad/Kokoro-82M/blob/main/VOICES.md
 */
private fun kokoroOverallQuality(id: String): String? = when (id) {
    "af_heart" -> "A"
    "af_alloy", "af_nova", "bf_isabella", "jf_gongitsune",
    "hf_alpha", "hf_beta", "hm_omega", "hm_psi", "if_sara", "im_nicola",
    "bm_fable", "bm_george", "jf_tebukuro",
    -> "C"
    "af_aoede", "af_kore", "af_sarah", "am_fenrir", "am_michael", "am_puck",
    "jf_alpha",
    -> "C+"
    "af_bella" -> "A-"
    "af_nicole", "bf_emma", "ff_siwis" -> "B-"
    "af_jessica", "af_river", "am_echo", "am_eric", "am_liam", "am_onyx",
    "bf_alice", "bf_lily", "bm_daniel", "zf_xiaobei", "zf_xiaoni", "zf_xiaoxiao",
    "zf_xiaoyi", "zm_yunjian", "zm_yunxi", "zm_yunxia", "zm_yunyang",
    -> "D"
    "af_sky", "jf_nezumi", "jm_kumo" -> "C-"
    "am_adam" -> "F+"
    "am_santa" -> "D-"
    "bm_lewis" -> "D+"
    else -> null
}
