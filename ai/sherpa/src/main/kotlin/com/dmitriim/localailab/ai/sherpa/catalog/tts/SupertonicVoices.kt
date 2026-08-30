package com.dmitriim.localailab.ai.sherpa.catalog.tts

/**
 * The pinned sherpa-onnx export sorts the official JSON filenames before packing voice.bin.
 * That makes the speaker order F1..F5 followed by M1..M5.
 *
 * Generated with sherpa-onnx's upstream Supertonic `generate_voices_bin.py` script.
 * https://huggingface.co/Supertone/supertonic-3/tree/main/voice_styles
 */
internal fun supertonicV3Voices() = listOf(
    supertonicVoice("F1", 0),
    supertonicVoice("F2", 1),
    supertonicVoice("F3", 2),
    supertonicVoice("F4", 3),
    supertonicVoice("F5", 4),
    supertonicVoice("M1", 5),
    supertonicVoice("M2", 6),
    supertonicVoice("M3", 7),
    supertonicVoice("M4", 8),
    supertonicVoice("M5", 9),
)

private fun supertonicVoice(id: String, speakerId: Int) = ttsVoice(
    id = id,
    displayName = id,
    speakerId = speakerId,
    description = "${if (id.startsWith('F')) "Female" else "Male"} · Multilingual",
    languages = arrayOf("en", "ru", "de"),
)
