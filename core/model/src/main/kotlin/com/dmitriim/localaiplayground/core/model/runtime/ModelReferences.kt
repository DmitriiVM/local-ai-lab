package com.dmitriim.localaiplayground.core.model.runtime

import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.manifest.ModelFileRole
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileId
import com.dmitriim.localaiplayground.core.model.manifest.SttRecognitionMode
import com.dmitriim.localaiplayground.core.model.manifest.TtsControl
import com.dmitriim.localaiplayground.core.model.manifest.TtsVoiceMode

data class ChatModelReference(
    val modelId: ModelId,
    val displayName: String,
    val profileType: ModelProfileId,
    val modelPath: String,
    val defaultContextSize: Int,
)

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
