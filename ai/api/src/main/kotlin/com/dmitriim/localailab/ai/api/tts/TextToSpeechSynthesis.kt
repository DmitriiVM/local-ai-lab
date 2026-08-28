package com.dmitriim.localailab.ai.api.tts

/** One request to synthesize text into mono PCM audio. */
data class TextToSpeechRequest(
    val text: String,
    val languageCode: String,
    val voice: TextToSpeechVoiceCondition,
    val speed: Float,
    val sentenceSilenceScale: Float,
)

/** Optional runtime-reported timings, counts, and memory observations for one synthesis. */
data class TextToSpeechStageMetrics(
    val conditioningDurationMs: Long? = null,
    val tokenGenerationDurationMs: Long? = null,
    val decoderDurationMs: Long? = null,
    val generatedTokenCount: Int? = null,
    val conditioningCacheHit: Boolean? = null,
    val peakProcessPssBytes: Long? = null,
    val availableDeviceMemoryBytes: Long? = null,
)

/**
 * Complete synthesized mono PCM audio and its format.
 *
 * [samples] is owned by the result. Callers may retain it after synthesis returns.
 */
class TextToSpeechResult(
    val samples: FloatArray,
    val sampleRateHz: Int,
    val stageMetrics: TextToSpeechStageMetrics = TextToSpeechStageMetrics(),
)
