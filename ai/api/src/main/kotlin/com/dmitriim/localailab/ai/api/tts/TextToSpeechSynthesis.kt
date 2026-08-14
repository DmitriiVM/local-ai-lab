package com.dmitriim.localailab.ai.api.tts

data class TextToSpeechRequest(
    val text: String,
    val languageCode: String,
    val voice: TextToSpeechVoiceCondition,
    val speed: Float,
    val sentenceSilenceScale: Float,
)

data class TextToSpeechStageMetrics(
    val conditioningDurationMs: Long? = null,
    val tokenGenerationDurationMs: Long? = null,
    val decoderDurationMs: Long? = null,
    val generatedTokenCount: Int? = null,
    val conditioningCacheHit: Boolean? = null,
    val peakProcessPssBytes: Long? = null,
    val availableDeviceMemoryBytes: Long? = null,
)

class TextToSpeechResult(
    val samples: FloatArray,
    val sampleRateHz: Int,
    val stageMetrics: TextToSpeechStageMetrics = TextToSpeechStageMetrics(),
)
