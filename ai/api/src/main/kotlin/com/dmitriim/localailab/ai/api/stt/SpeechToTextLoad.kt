package com.dmitriim.localailab.ai.api.stt

import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelFileRole
import com.dmitriim.localailab.core.model.manifest.ModelProfileId

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
