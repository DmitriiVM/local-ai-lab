package com.dmitriim.localailab.feature.models.api.domain.runtime

import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.model.manifest.ModelProfileId
import com.dmitriim.localailab.core.model.manifest.TtsControl
import com.dmitriim.localailab.core.model.manifest.TtsVoiceMode
import com.dmitriim.localailab.core.model.runtime.ModelArtifactReference

/** A validated TTS model location and configuration resolved for use by a TTS feature. */
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
