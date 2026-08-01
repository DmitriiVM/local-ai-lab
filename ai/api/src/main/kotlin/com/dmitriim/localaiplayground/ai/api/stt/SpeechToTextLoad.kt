package com.dmitriim.localaiplayground.ai.api.stt

import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.manifest.ModelFileRole
import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileId

data class SpeechToTextLoadRequest(
    val engineId: EngineId,
    val profileType: ModelProfileId,
    val modelDirectory: String,
    val files: Map<ModelFileRole, String>,
    val languageCode: String,
    /** Zero selects an engine-safe default. */
    val threadCount: Int = 0,
)

data class SpeechToTextLoadResult(
    val effectiveThreadCount: Int,
    val loadDurationMs: Long,
    val coldStart: Boolean,
)
