package com.dmitriim.localailab.ai.api.stt

import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelFileRole
import com.dmitriim.localailab.core.model.manifest.ModelProfileId
import com.dmitriim.localailab.core.model.runtime.ModelArtifactReference

data class SpeechToTextLoadRequest(
    val engineId: EngineId,
    val profileType: ModelProfileId,
    val modelDirectory: String,
    val artifacts: List<ModelArtifactReference>,
    val languageCode: String,
    /** Zero selects an engine-safe default. */
    val threadCount: Int = 0,
) {
    /** Compatibility constructor for callers that still provide one file per role. */
    constructor(
        engineId: EngineId,
        profileType: ModelProfileId,
        modelDirectory: String,
        files: Map<ModelFileRole, String>,
        languageCode: String,
        threadCount: Int = 0,
    ) : this(
        engineId = engineId,
        profileType = profileType,
        modelDirectory = modelDirectory,
        artifacts = files.map { (role, path) -> ModelArtifactReference(role = role, path = path) },
        languageCode = languageCode,
        threadCount = threadCount,
    )

    val files: Map<ModelFileRole, String>
        get() = artifacts
            .filterNot(ModelArtifactReference::directory)
            .associate { it.role to it.path }
}

data class SpeechToTextLoadResult(
    val effectiveThreadCount: Int,
    val loadDurationMs: Long,
    val coldStart: Boolean,
)
