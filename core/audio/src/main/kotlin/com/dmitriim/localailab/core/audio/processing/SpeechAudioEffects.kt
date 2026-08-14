package com.dmitriim.localailab.core.audio.processing

import kotlin.math.abs

data class SpeechAudioEffects(
    val pitchSemitones: Float = 0f,
    val formantSemitones: Float = 0f,
    val lowEqDb: Float = 0f,
    val midEqDb: Float = 0f,
    val highEqDb: Float = 0f,
    val saturationDriveDb: Float = 0f,
) {
    val isNeutral: Boolean
        get() = abs(pitchSemitones) < NEUTRAL_EPSILON &&
            abs(formantSemitones) < NEUTRAL_EPSILON &&
            abs(lowEqDb) < NEUTRAL_EPSILON &&
            abs(midEqDb) < NEUTRAL_EPSILON &&
            abs(highEqDb) < NEUTRAL_EPSILON &&
            saturationDriveDb < NEUTRAL_EPSILON

    fun validate() {
        require(pitchSemitones in PITCH_RANGE) {
            "Pitch must be between ${PITCH_RANGE.start} and ${PITCH_RANGE.endInclusive} semitones."
        }
        require(formantSemitones in FORMANT_RANGE) {
            "Formant shift must be between ${FORMANT_RANGE.start} and ${FORMANT_RANGE.endInclusive} semitones."
        }
        require(lowEqDb in EQ_RANGE && midEqDb in EQ_RANGE && highEqDb in EQ_RANGE) {
            "Equalizer bands must be between ${EQ_RANGE.start} and ${EQ_RANGE.endInclusive} dB."
        }
        require(saturationDriveDb in SATURATION_RANGE) {
            "Saturation drive must be between ${SATURATION_RANGE.start} and ${SATURATION_RANGE.endInclusive} dB."
        }
    }

    companion object {
        val PITCH_RANGE = -12f..12f
        val FORMANT_RANGE = -6f..6f
        val EQ_RANGE = -12f..12f
        val SATURATION_RANGE = 0f..24f
        private const val NEUTRAL_EPSILON = 0.001f
    }
}
