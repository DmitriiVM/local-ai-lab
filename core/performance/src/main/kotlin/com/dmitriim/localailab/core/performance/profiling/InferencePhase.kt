package com.dmitriim.localailab.core.performance.profiling

import kotlinx.serialization.Serializable

@Serializable
enum class InferencePhase {
    TOTAL,
    MODEL_RESOLUTION,
    MODEL_LOAD,
    PROMPT_PREPARATION,
    DECODE,
    TRANSCRIPTION,
    SYNTHESIS,
    AUDIO_EFFECTS,
    AUDIO_PLAYBACK,
}
