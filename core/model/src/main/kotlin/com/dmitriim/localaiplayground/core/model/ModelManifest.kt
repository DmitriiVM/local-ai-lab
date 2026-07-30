package com.dmitriim.localaiplayground.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ModelFormat {
    GGUF,
    ONNX,
    BINARY,
    ARCHIVE,
}

/** Stable, adapter-owned runtime profile identifier. Values remain extensible across app releases. */
@Serializable
@JvmInline
value class ModelProfileId(val value: String) {
    companion object {
        val LLM = ModelProfileId("LLM")
        val WHISPER_STT = ModelProfileId("WHISPER_STT")
        val PARAKEET_CTC_STT = ModelProfileId("PARAKEET_CTC_STT")
        val GIGAAM_CTC_STT = ModelProfileId("GIGAAM_CTC_STT")
        val ZIPFORMER_STT = ModelProfileId("ZIPFORMER_STT")
        val SENSE_VOICE_STT = ModelProfileId("SENSE_VOICE_STT")
        val PARAFORMER_STT = ModelProfileId("PARAFORMER_STT")
        val MOONSHINE_STT = ModelProfileId("MOONSHINE_STT")
        val VOSK_STT = ModelProfileId("VOSK_STT")
        val ANDROID_SPEECH_RECOGNIZER_STT = ModelProfileId("ANDROID_SPEECH_RECOGNIZER_STT")
        val SUPERTONIC_TTS = ModelProfileId("SUPERTONIC_TTS")
        val PIPER_VITS_TTS = ModelProfileId("PIPER_VITS_TTS")
        val KOKORO_TTS = ModelProfileId("KOKORO_TTS")
        val POCKET_TTS = ModelProfileId("POCKET_TTS")
        val CHATTERBOX_TURBO_Q4 = ModelProfileId("CHATTERBOX_TURBO_Q4")
    }
}

/** Existing persisted identifiers plus the Piper/VITS proof profile. */
object ModelProfileIds {
    val LLM = ModelProfileId("LLM")
    val WHISPER_STT = ModelProfileId("WHISPER_STT")
    val PARAKEET_CTC_STT = ModelProfileId("PARAKEET_CTC_STT")
    val GIGAAM_CTC_STT = ModelProfileId("GIGAAM_CTC_STT")
    val ZIPFORMER_STT = ModelProfileId("ZIPFORMER_STT")
    val SENSE_VOICE_STT = ModelProfileId("SENSE_VOICE_STT")
    val PARAFORMER_STT = ModelProfileId("PARAFORMER_STT")
    val MOONSHINE_STT = ModelProfileId("MOONSHINE_STT")
    val VOSK_STT = ModelProfileId("VOSK_STT")
    val ANDROID_SPEECH_RECOGNIZER_STT = ModelProfileId("ANDROID_SPEECH_RECOGNIZER_STT")
    val SUPERTONIC_TTS = ModelProfileId("SUPERTONIC_TTS")
    val PIPER_VITS_TTS = ModelProfileId("PIPER_VITS_TTS")
    val KOKORO_TTS = ModelProfileId("KOKORO_TTS")
    val POCKET_TTS = ModelProfileId("POCKET_TTS")
    val CHATTERBOX_TURBO_Q4 = ModelProfileId("CHATTERBOX_TURBO_Q4")
}

/** Semantic role of an installed model file. Adapters may define additional roles. */
@Serializable
@JvmInline
value class ModelFileRole(val value: String) {
    companion object {
        val PRIMARY_MODEL = ModelFileRole("PRIMARY_MODEL")
        val ENCODER = ModelFileRole("ENCODER")
        val DECODER = ModelFileRole("DECODER")
        val JOINER = ModelFileRole("JOINER")
        val MERGED_DECODER = ModelFileRole("MERGED_DECODER")
        val TOKENS = ModelFileRole("TOKENS")
        val DURATION_PREDICTOR = ModelFileRole("DURATION_PREDICTOR")
        val TEXT_ENCODER = ModelFileRole("TEXT_ENCODER")
        val VECTOR_ESTIMATOR = ModelFileRole("VECTOR_ESTIMATOR")
        val VOCODER = ModelFileRole("VOCODER")
        val CONFIG = ModelFileRole("CONFIG")
        val UNICODE_INDEXER = ModelFileRole("UNICODE_INDEXER")
        val VOICE_STYLE = ModelFileRole("VOICE_STYLE")
        val LICENSE = ModelFileRole("LICENSE")
        val VITS_MODEL = ModelFileRole("VITS_MODEL")
        val FRONTEND_DATA = ModelFileRole("FRONTEND_DATA")
        val KOKORO_MODEL = ModelFileRole("KOKORO_MODEL")
        val VOICE_EMBEDDINGS = ModelFileRole("VOICE_EMBEDDINGS")
        val LEXICON = ModelFileRole("LEXICON")
        val TEXT_RULES = ModelFileRole("TEXT_RULES")
        val DICTIONARY_DATA = ModelFileRole("DICTIONARY_DATA")
        val LM_FLOW = ModelFileRole("LM_FLOW")
        val LM_MAIN = ModelFileRole("LM_MAIN")
        val POCKET_ENCODER = ModelFileRole("POCKET_ENCODER")
        val POCKET_DECODER = ModelFileRole("POCKET_DECODER")
        val TEXT_CONDITIONER = ModelFileRole("TEXT_CONDITIONER")
        val VOCABULARY = ModelFileRole("VOCABULARY")
        val TOKEN_SCORES = ModelFileRole("TOKEN_SCORES")
        val REFERENCE_AUDIO = ModelFileRole("REFERENCE_AUDIO")
        val SPEECH_ENCODER = ModelFileRole("SPEECH_ENCODER")
        val EMBED_TOKENS = ModelFileRole("EMBED_TOKENS")
        val LANGUAGE_MODEL = ModelFileRole("LANGUAGE_MODEL")
        val CONDITIONAL_DECODER = ModelFileRole("CONDITIONAL_DECODER")
        val EXTERNAL_DATA = ModelFileRole("EXTERNAL_DATA")
        val TOKENIZER = ModelFileRole("TOKENIZER")
    }
}

@Suppress("unused")
private val _legacyRoleCompatibility = Unit

object ModelFileRoles {
    val PRIMARY_MODEL = ModelFileRole("PRIMARY_MODEL")
    val ENCODER = ModelFileRole("ENCODER")
    val DECODER = ModelFileRole("DECODER")
    val JOINER = ModelFileRole("JOINER")
    val MERGED_DECODER = ModelFileRole("MERGED_DECODER")
    val TOKENS = ModelFileRole("TOKENS")
    val DURATION_PREDICTOR = ModelFileRole("DURATION_PREDICTOR")
    val TEXT_ENCODER = ModelFileRole("TEXT_ENCODER")
    val VECTOR_ESTIMATOR = ModelFileRole("VECTOR_ESTIMATOR")
    val VOCODER = ModelFileRole("VOCODER")
    val CONFIG = ModelFileRole("CONFIG")
    val UNICODE_INDEXER = ModelFileRole("UNICODE_INDEXER")
    val VOICE_STYLE = ModelFileRole("VOICE_STYLE")
    val LICENSE = ModelFileRole("LICENSE")
    val VITS_MODEL = ModelFileRole("VITS_MODEL")
    val FRONTEND_DATA = ModelFileRole("FRONTEND_DATA")
    val KOKORO_MODEL = ModelFileRole("KOKORO_MODEL")
    val VOICE_EMBEDDINGS = ModelFileRole("VOICE_EMBEDDINGS")
    val LEXICON = ModelFileRole("LEXICON")
    val TEXT_RULES = ModelFileRole("TEXT_RULES")
    val DICTIONARY_DATA = ModelFileRole("DICTIONARY_DATA")
    val LM_FLOW = ModelFileRole("LM_FLOW")
    val LM_MAIN = ModelFileRole("LM_MAIN")
    val POCKET_ENCODER = ModelFileRole("POCKET_ENCODER")
    val POCKET_DECODER = ModelFileRole("POCKET_DECODER")
    val TEXT_CONDITIONER = ModelFileRole("TEXT_CONDITIONER")
    val VOCABULARY = ModelFileRole("VOCABULARY")
    val TOKEN_SCORES = ModelFileRole("TOKEN_SCORES")
    val REFERENCE_AUDIO = ModelFileRole("REFERENCE_AUDIO")
    val SPEECH_ENCODER = ModelFileRole("SPEECH_ENCODER")
    val EMBED_TOKENS = ModelFileRole("EMBED_TOKENS")
    val LANGUAGE_MODEL = ModelFileRole("LANGUAGE_MODEL")
    val CONDITIONAL_DECODER = ModelFileRole("CONDITIONAL_DECODER")
    val EXTERNAL_DATA = ModelFileRole("EXTERNAL_DATA")
    val TOKENIZER = ModelFileRole("TOKENIZER")
}

@Serializable
enum class TtsVoiceMode {
    SPEAKER_ID,
    REFERENCE_AUDIO,
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

object BuiltInSpeechToTextModels {
    val ANDROID_SPEECH_RECOGNIZER = ModelId("android-on-device-speech-recognizer")
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
