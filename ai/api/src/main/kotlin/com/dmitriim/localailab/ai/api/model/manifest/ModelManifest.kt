package com.dmitriim.localailab.ai.api.model.manifest

import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.engine.EngineId
import kotlinx.serialization.Serializable

@Serializable
enum class ModelFormat {
    GGUF,
    LITERT_LM,
    ONNX,
    BINARY,
    ARCHIVE,
}

@Serializable
enum class TtsVoiceMode {
    SPEAKER_ID,
    REFERENCE_AUDIO,
    PLATFORM,
}

@Serializable
enum class TtsControl {
    LANGUAGE,
    SPEAKER,
    SPEECH_RATE,
    SENTENCE_SILENCE,
    REFERENCE_VOICE,
    EXPRESSIVE_TAGS,
}

@Serializable
enum class SttRecognitionMode {
    OFFLINE,
    STREAMING,
}

@Serializable
data class ModelFileSpec(
    val relativePath: String,
    val role: ModelFileRole,
    val expectedBytes: Long? = null,
    val sha256: String? = null,
    val required: Boolean = true,
    /** Frontend bundles may be a recursively copied directory rather than a single file. */
    val directory: Boolean = false,
)

@Serializable
data class ModelSource(
    val url: String?,
    val revision: String? = null,
    val licenseName: String,
    val attribution: String,
)

/** A stable, user-facing voice mapped to the speaker index expected by a TTS runtime. */
@Serializable
data class TtsVoiceDescriptor(
    val id: String,
    val displayName: String,
    val speakerId: Int,
    val languages: Set<String> = emptySet(),
    /** Display-only upstream metadata. Selection and synthesis logic must not parse this value. */
    val description: String? = null,
)

/** Versioned manifest stored beside a model bundle in app-private storage. */
@Serializable
data class ModelManifest(
    val schemaVersion: Int = 2,
    val modelId: ModelId,
    val displayName: String,
    val family: String,
    val description: String? = null,
    val capabilities: Set<AiCapability>,
    val engineId: EngineId,
    /** Kept under its original serialized field name to retain installed-manifest compatibility. */
    val profileType: ModelProfileId,
    val format: ModelFormat,
    val quantization: String? = null,
    val architecture: String? = null,
    val revision: String? = null,
    val files: List<ModelFileSpec>,
    val source: ModelSource,
    val languages: Set<String> = emptySet(),
    /** Total verified language coverage when [languages] contains only representative languages for display. */
    val supportedLanguageCount: Int? = null,
    val sampleRateHz: Int? = null,
    val speakerCount: Int? = null,
    val voices: List<TtsVoiceDescriptor> = emptyList(),
    /** Defaults preserve schema-v2 manifests installed before reference-voice TTS existed. */
    val ttsVoiceMode: TtsVoiceMode = TtsVoiceMode.SPEAKER_ID,
    val ttsControls: Set<TtsControl> = setOf(
        TtsControl.LANGUAGE,
        TtsControl.SPEAKER,
        TtsControl.SPEECH_RATE,
        TtsControl.SENTENCE_SILENCE,
    ),
    /** Describes the native recognizer. The current STT screen still emits one final result per segment. */
    val sttRecognitionMode: SttRecognitionMode = SttRecognitionMode.OFFLINE,
    val contextSize: Int? = null,
    val approximateRamBytes: Long? = null,
    val catalogVersion: Int? = null,
    val installedAtEpochMs: Long,
)
