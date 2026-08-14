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
    val files: Map<ModelFileRole, String>,
    val sampleRateHz: Int,
    val languages: Set<String>,
    val recognitionMode: SttRecognitionMode,
)

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
)
