package com.dmitriim.localailab.core.audio.input.model

import java.io.File

data class PcmAudioInput(
    val file: File,
    val displayName: String,
    val durationMs: Long,
    val sampleRateHz: Int,
    val sourceDescription: String,
)

data class AudioLevel(val elapsedMs: Long, val peak: Float, val rms: Float)

internal const val STT_SAMPLE_RATE_HZ = 16_000
