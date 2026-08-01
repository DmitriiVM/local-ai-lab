package com.dmitriim.localaiplayground.ai.sherpa

import com.dmitriim.localaiplayground.ai.api.model.ModelAdapter
import com.dmitriim.localaiplayground.ai.api.model.ModelImportDefinition
import com.dmitriim.localaiplayground.ai.api.model.ModelImportFileDefinition
import com.dmitriim.localaiplayground.ai.api.model.RuntimeValidationResult
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.manifest.ModelFileRoles
import com.dmitriim.localaiplayground.core.model.manifest.ModelFormat
import com.dmitriim.localaiplayground.core.model.manifest.ModelManifest
import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileId
import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileIds
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import java.io.File

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelAdapter>())
class SherpaModelRuntimeValidator : ModelAdapter {
    override val id = "sherpa-onnx-families"
    override val engineId = EngineId("sherpa-onnx")
    override val profileTypes = setOf(
        ModelProfileIds.WHISPER_STT,
        ModelProfileIds.PARAKEET_CTC_STT,
        ModelProfileIds.GIGAAM_CTC_STT,
        ModelProfileIds.ZIPFORMER_STT,
        ModelProfileIds.SENSE_VOICE_STT,
        ModelProfileIds.PARAFORMER_STT,
        ModelProfileIds.MOONSHINE_STT,
        ModelProfileIds.SUPERTONIC_TTS,
        ModelProfileIds.PIPER_VITS_TTS,
        ModelProfileIds.KOKORO_TTS,
        ModelProfileIds.POCKET_TTS,
    )
    override val capabilities = setOf(
        AiCapability.SPEECH_TO_TEXT,
        AiCapability.TEXT_TO_SPEECH,
    )

    override fun capabilitiesFor(profileType: ModelProfileId) = when (profileType) {
        ModelProfileIds.WHISPER_STT,
        ModelProfileIds.PARAKEET_CTC_STT,
        ModelProfileIds.GIGAAM_CTC_STT,
        ModelProfileIds.ZIPFORMER_STT,
        ModelProfileIds.SENSE_VOICE_STT,
        ModelProfileIds.PARAFORMER_STT,
        ModelProfileIds.MOONSHINE_STT,
        -> setOf(AiCapability.SPEECH_TO_TEXT)
        ModelProfileIds.SUPERTONIC_TTS,
        ModelProfileIds.PIPER_VITS_TTS,
        ModelProfileIds.KOKORO_TTS,
        ModelProfileIds.POCKET_TTS,
        -> setOf(AiCapability.TEXT_TO_SPEECH)
        else -> emptySet()
    }

    override fun importDefinition(profileType: ModelProfileId) = when (profileType) {
        ModelProfileIds.WHISPER_STT -> ModelImportDefinition(
            displayName = "Whisper STT bundle",
            format = ModelFormat.ONNX,
            files = listOf(
                ModelImportFileDefinition(ModelFileRoles.ENCODER, relativePath = "base-encoder.int8.onnx"),
                ModelImportFileDefinition(ModelFileRoles.DECODER, relativePath = "base-decoder.int8.onnx"),
                ModelImportFileDefinition(ModelFileRoles.TOKENS, relativePath = "base-tokens.txt"),
            ),
        )
        ModelProfileIds.PARAKEET_CTC_STT,
        ModelProfileIds.GIGAAM_CTC_STT,
        ModelProfileIds.SENSE_VOICE_STT,
        ModelProfileIds.PARAFORMER_STT,
        -> ModelImportDefinition(
            displayName = "Single-model STT bundle",
            format = ModelFormat.ONNX,
            files = listOf(
                ModelImportFileDefinition(ModelFileRoles.PRIMARY_MODEL, relativePath = "model.int8.onnx"),
                ModelImportFileDefinition(ModelFileRoles.TOKENS, relativePath = "tokens.txt"),
            ),
        )
        ModelProfileIds.ZIPFORMER_STT -> ModelImportDefinition(
            displayName = "Streaming Zipformer bundle",
            format = ModelFormat.ONNX,
            files = listOf(
                ModelImportFileDefinition(ModelFileRoles.ENCODER, relativePath = "encoder-epoch-99-avg-1.int8.onnx"),
                ModelImportFileDefinition(ModelFileRoles.DECODER, relativePath = "decoder-epoch-99-avg-1.int8.onnx"),
                ModelImportFileDefinition(ModelFileRoles.JOINER, relativePath = "joiner-epoch-99-avg-1.int8.onnx"),
                ModelImportFileDefinition(ModelFileRoles.TOKENS, relativePath = "tokens.txt"),
            ),
        )
        ModelProfileIds.MOONSHINE_STT -> ModelImportDefinition(
            displayName = "Moonshine STT bundle",
            format = ModelFormat.ONNX,
            files = listOf(
                ModelImportFileDefinition(ModelFileRoles.ENCODER, relativePath = "encoder_model.ort"),
                ModelImportFileDefinition(ModelFileRoles.MERGED_DECODER, relativePath = "decoder_model_merged.ort"),
                ModelImportFileDefinition(ModelFileRoles.TOKENS, relativePath = "tokens.txt"),
            ),
        )
        ModelProfileIds.SUPERTONIC_TTS -> ModelImportDefinition(
            displayName = "Supertonic TTS bundle",
            format = ModelFormat.ONNX,
            files = listOf(
                ModelImportFileDefinition(ModelFileRoles.DURATION_PREDICTOR, relativePath = "duration_predictor.int8.onnx"),
                ModelImportFileDefinition(ModelFileRoles.TEXT_ENCODER, relativePath = "text_encoder.int8.onnx"),
                ModelImportFileDefinition(ModelFileRoles.VECTOR_ESTIMATOR, relativePath = "vector_estimator.int8.onnx"),
                ModelImportFileDefinition(ModelFileRoles.VOCODER, relativePath = "vocoder.int8.onnx"),
                ModelImportFileDefinition(ModelFileRoles.CONFIG, relativePath = "tts.json"),
                ModelImportFileDefinition(ModelFileRoles.UNICODE_INDEXER, relativePath = "unicode_indexer.bin"),
                ModelImportFileDefinition(ModelFileRoles.VOICE_STYLE, relativePath = "voice.bin"),
            ),
        )
        ModelProfileIds.PIPER_VITS_TTS -> ModelImportDefinition(
            displayName = "Piper Lessac Medium (English)",
            format = ModelFormat.ONNX,
            files = listOf(
                ModelImportFileDefinition(ModelFileRoles.VITS_MODEL, relativePath = "en_US-lessac-medium.onnx"),
                ModelImportFileDefinition(ModelFileRoles.TOKENS, relativePath = "tokens.txt"),
                ModelImportFileDefinition(ModelFileRoles.FRONTEND_DATA, relativePath = "espeak-ng-data", directory = true),
            ),
        )
        ModelProfileIds.KOKORO_TTS -> ModelImportDefinition(
            displayName = "Kokoro Multi-Lang v1.0",
            format = ModelFormat.ONNX,
            files = listOf(
                ModelImportFileDefinition(ModelFileRoles.KOKORO_MODEL, relativePath = "model.onnx"),
                ModelImportFileDefinition(ModelFileRoles.VOICE_EMBEDDINGS, relativePath = "voices.bin"),
                ModelImportFileDefinition(ModelFileRoles.TOKENS, relativePath = "tokens.txt"),
                ModelImportFileDefinition(ModelFileRoles.LEXICON, relativePath = "lexicon-us-en.txt"),
                ModelImportFileDefinition(ModelFileRoles.LEXICON, relativePath = "lexicon-zh.txt"),
                ModelImportFileDefinition(ModelFileRoles.TEXT_RULES, relativePath = "date-zh.fst"),
                ModelImportFileDefinition(ModelFileRoles.TEXT_RULES, relativePath = "number-zh.fst"),
                ModelImportFileDefinition(ModelFileRoles.TEXT_RULES, relativePath = "phone-zh.fst"),
                ModelImportFileDefinition(ModelFileRoles.FRONTEND_DATA, relativePath = "espeak-ng-data", directory = true),
                ModelImportFileDefinition(ModelFileRoles.DICTIONARY_DATA, relativePath = "dict", directory = true),
            ),
        )
        ModelProfileIds.POCKET_TTS -> ModelImportDefinition(
            displayName = "Pocket TTS INT8 (English)",
            format = ModelFormat.ONNX,
            files = listOf(
                ModelImportFileDefinition(ModelFileRoles.LM_FLOW, relativePath = "lm_flow.int8.onnx"),
                ModelImportFileDefinition(ModelFileRoles.LM_MAIN, relativePath = "lm_main.int8.onnx"),
                ModelImportFileDefinition(ModelFileRoles.POCKET_ENCODER, relativePath = "encoder.onnx"),
                ModelImportFileDefinition(ModelFileRoles.POCKET_DECODER, relativePath = "decoder.int8.onnx"),
                ModelImportFileDefinition(ModelFileRoles.TEXT_CONDITIONER, relativePath = "text_conditioner.onnx"),
                ModelImportFileDefinition(ModelFileRoles.VOCABULARY, relativePath = "vocab.json"),
                ModelImportFileDefinition(ModelFileRoles.TOKEN_SCORES, relativePath = "token_scores.json"),
                ModelImportFileDefinition(ModelFileRoles.REFERENCE_AUDIO, relativePath = "test_wavs/bria.wav"),
            ),
        )
        else -> null
    }

    override fun validate(manifest: ModelManifest, directory: File): RuntimeValidationResult = runCatching {
        val missing = manifest.files.filter { it.required }
            .map { File(directory, it.relativePath) }
            .filterNot { it.canRead() && (it.isFile || it.isDirectory) }
        require(missing.isEmpty()) { "Missing required files: ${missing.joinToString { it.name }}" }
        require(manifest.profileType in profileTypes) { "Unsupported sherpa-onnx profile: ${manifest.profileType.value}" }
    }.fold(
        onSuccess = { RuntimeValidationResult(valid = true) },
        onFailure = { RuntimeValidationResult(valid = false, message = it.message) },
    )
}
