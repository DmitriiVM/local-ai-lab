package com.dmitriim.localailab.core.model.runtime

import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelFileRole
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.model.manifest.ModelProfileId
import com.dmitriim.localailab.core.model.manifest.SttRecognitionMode
import com.dmitriim.localailab.core.model.manifest.TtsControl
import com.dmitriim.localailab.core.model.manifest.TtsVoiceMode

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

data class TextToSpeechModelReference(
    val modelId: ModelId,
    val displayName: String,
    val engineId: EngineId,
    val profileType: ModelProfileId,
    val modelDirectory: String,
    val sampleRateHz: Int,
    val languages: Set<String>,
    val speakerCount: Int?,
    val voiceMode: TtsVoiceMode,
    val supportedControls: Set<TtsControl>,
    val artifacts: List<ModelArtifactReference> = emptyList(),
)
