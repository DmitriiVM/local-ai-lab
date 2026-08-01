package com.dmitriim.localaiplayground.ai.api.tts

import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileId

data class TextToSpeechLoadRequest(
    val engineId: EngineId,
    val profileType: ModelProfileId,
    val modelDirectory: String,
    /** Zero selects an engine-safe default. */
    val threadCount: Int = 0,
)

data class TextToSpeechLoadResult(
    val effectiveThreadCount: Int,
    val loadDurationMs: Long,
    val coldStart: Boolean,
    /** Zero when the backend reports its output format only after synthesis begins. */
    val sampleRateHz: Int,
    val speakerCount: Int?,
)
