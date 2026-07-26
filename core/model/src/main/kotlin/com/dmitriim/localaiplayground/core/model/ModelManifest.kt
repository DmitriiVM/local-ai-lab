package com.dmitriim.localaiplayground.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ModelFormat {
    GGUF,
    ONNX,
    BINARY,
    ARCHIVE,
}

@Serializable
enum class RuntimeProfileType {
    LLM,
    WHISPER_STT,
    SILERO_VAD,
    SUPERTONIC_TTS,
}

@Serializable
enum class ModelFileRole {
    PRIMARY_MODEL,
    ENCODER,
    DECODER,
    TOKENS,
    VAD_MODEL,
    DURATION_PREDICTOR,
    TEXT_ENCODER,
    VECTOR_ESTIMATOR,
    VOCODER,
    CONFIG,
    UNICODE_INDEXER,
    VOICE_STYLE,
    LICENSE,
}

@Serializable
data class ModelFileSpec(
    val relativePath: String,
    val role: ModelFileRole,
    val expectedBytes: Long? = null,
    val sha256: String? = null,
    val required: Boolean = true,
)

@Serializable
data class ModelSource(
    val url: String?,
    val revision: String? = null,
    val licenseName: String,
    val attribution: String,
)

/** Versioned manifest stored beside a model bundle in app-private storage. */
@Serializable
data class ModelManifest(
    val schemaVersion: Int = 1,
    val modelId: ModelId,
    val displayName: String,
    val family: String,
    val capabilities: Set<AiCapability>,
    val engineId: EngineId,
    val profileType: RuntimeProfileType,
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
    val contextSize: Int? = null,
    val approximateRamBytes: Long? = null,
    val catalogVersion: Int? = null,
    val installedAtEpochMs: Long,
)
