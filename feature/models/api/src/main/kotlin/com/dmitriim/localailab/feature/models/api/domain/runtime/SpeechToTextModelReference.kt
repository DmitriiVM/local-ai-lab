package com.dmitriim.localailab.feature.models.api.domain.runtime

import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRole
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import com.dmitriim.localailab.ai.api.model.manifest.SttRecognitionMode
import com.dmitriim.localailab.ai.api.model.runtime.ModelArtifactReference

/** A validated STT model location and configuration resolved for use by an STT feature. */
data class SpeechToTextModelReference(
    val modelId: ModelId,
    val displayName: String,
    val engineId: EngineId,
    val profileType: ModelProfileId,
    val modelDirectory: String,
    val artifacts: List<ModelArtifactReference>,
    val sampleRateHz: Int,
    val languages: Set<String>,
    val recognitionMode: SttRecognitionMode,
) {
    /** Compatibility constructor for callers that still provide one file per role. */
    constructor(
        modelId: ModelId,
        displayName: String,
        engineId: EngineId,
        profileType: ModelProfileId,
        modelDirectory: String,
        files: Map<ModelFileRole, String>,
        sampleRateHz: Int,
        languages: Set<String>,
        recognitionMode: SttRecognitionMode,
    ) : this(
        modelId = modelId,
        displayName = displayName,
        engineId = engineId,
        profileType = profileType,
        modelDirectory = modelDirectory,
        artifacts = files.map { (role, path) -> ModelArtifactReference(role = role, path = path) },
        sampleRateHz = sampleRateHz,
        languages = languages,
        recognitionMode = recognitionMode,
    )

    val files: Map<ModelFileRole, String>
        get() = artifacts
            .filterNot(ModelArtifactReference::directory)
            .associate { it.role to it.path }
}
