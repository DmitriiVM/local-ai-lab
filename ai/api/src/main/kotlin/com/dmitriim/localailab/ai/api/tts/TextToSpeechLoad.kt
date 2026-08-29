package com.dmitriim.localailab.ai.api.tts

import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import com.dmitriim.localailab.ai.api.model.runtime.ModelArtifactReference

/** Selects an installed speech-synthesis model and optional runtime configuration. */
data class TextToSpeechLoadRequest(
    val engineId: EngineId,
    val profileType: ModelProfileId,
    val modelDirectory: String,
    /** Zero selects an engine-safe default. */
    val threadCount: Int = 0,
    val artifacts: List<ModelArtifactReference> = emptyList(),
)

/** Effective configuration and output metadata returned after a synthesizer has loaded. */
data class TextToSpeechLoadResult(
    val effectiveThreadCount: Int,
    val loadDurationMs: Long,
    val coldStart: Boolean,
    /** Zero when the runtime reports its output format only after synthesis begins. */
    val sampleRateHz: Int,
    val speakerCount: Int?,
)
