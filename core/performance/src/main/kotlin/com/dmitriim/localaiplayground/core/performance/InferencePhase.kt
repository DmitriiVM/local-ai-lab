package com.dmitriim.localaiplayground.core.performance

import kotlinx.serialization.Serializable

@Serializable
enum class InferencePhase {
    TOTAL,
    MODEL_RESOLUTION,
    MODEL_LOAD,
    PROMPT_PREPARATION,
    PREFILL,
    DECODE,
    AUDIO_SEGMENTATION,
    TRANSCRIPTION,
    CONDITIONING,
    TOKEN_GENERATION,
    SYNTHESIS,
    AUDIO_EFFECTS,
    AUDIO_PLAYBACK,
}
