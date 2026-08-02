package com.dmitriim.localaiplayground.source.models.catalog.tts

import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.library.CatalogDownload
import com.dmitriim.localaiplayground.core.model.library.CatalogDownloadFile
import com.dmitriim.localaiplayground.core.model.library.CatalogModel
import com.dmitriim.localaiplayground.core.model.library.ModelCatalogState
import com.dmitriim.localaiplayground.core.model.manifest.ModelFileRoles
import com.dmitriim.localaiplayground.core.model.manifest.ModelFileSpec
import com.dmitriim.localaiplayground.core.model.manifest.ModelFormat
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.model.manifest.ModelManifest
import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileIds
import com.dmitriim.localaiplayground.core.model.manifest.ModelSource
import com.dmitriim.localaiplayground.core.model.manifest.TtsControl
import com.dmitriim.localaiplayground.core.model.manifest.TtsVoiceMode
import com.dmitriim.localaiplayground.source.models.catalog.CatalogDefaults

private const val chatterboxRepository = "ResembleAI/chatterbox-turbo-ONNX"
private const val chatterboxRevision = "7232a922b8b11a00f473f0a4d2ec233c2148c905"

internal val chatterboxCatalogEntry: CatalogModel = CatalogModel(
    manifest = ModelManifest(
        modelId = ModelId("chatterbox-turbo-q4-en"),
        displayName = "Chatterbox Turbo Q4 (English)",
        family = "Chatterbox Turbo",
        description = "An English text-to-speech model that uses reference audio and supports expressive tags.",
        capabilities = setOf(AiCapability.TEXT_TO_SPEECH),
        engineId = EngineId("chatterbox-onnx"),
        profileType = ModelProfileIds.CHATTERBOX_TURBO_Q4,
        format = ModelFormat.ONNX,
        quantization = "Q4",
        architecture = "Chatterbox Turbo 350M",
        revision = chatterboxRevision,
        files = listOf(
            ModelFileSpec("conditional_decoder_q4.onnx", ModelFileRoles.CONDITIONAL_DECODER, 2_179_022, "dccb7a6cea3472dc7f7d070eeb70ade18e6327fb4ec61a3d62cf211bfed90ea2"),
            ModelFileSpec("conditional_decoder_q4.onnx_data", ModelFileRoles.EXTERNAL_DATA, 246_397_384, "b5c5317e0b79a1a19dd3d5e2b2091ea06b15716716ab801a54eaeb906c6971ec"),
            ModelFileSpec("embed_tokens_q4.onnx", ModelFileRoles.EMBED_TOKENS, 2_844, "fd6ba1d22902e8f539d3dd6d7c1c44b98ebb4c84ebbb5e47fcb826ddcf667561"),
            ModelFileSpec("embed_tokens_q4.onnx_data", ModelFileRoles.EXTERNAL_DATA, 37_286_384, "f54a51e234b509b64c3a03bb79e1149fba7e2eba6c2d9c222f18883379e1f5d8"),
            ModelFileSpec("language_model_q4.onnx", ModelFileRoles.LANGUAGE_MODEL, 274_572, "b39d03d3f8b943b9e60c6fce3fb41191dbc1df4589f913291db1e214eef669b1"),
            ModelFileSpec("language_model_q4.onnx_data", ModelFileRoles.EXTERNAL_DATA, 204_456_572, "2c029dc0acf48752473d8c74c72b5ceaaad76b9886fe106eaf2022142d5b5d5e"),
            ModelFileSpec("speech_encoder_q4.onnx", ModelFileRoles.SPEECH_ENCODER, 1_200_346, "37956c20b67bed85a0da4bc83509d67b5969a1b257d1c546516a5236a17ad71e"),
            ModelFileSpec("speech_encoder_q4.onnx_data", ModelFileRoles.EXTERNAL_DATA, 229_560_112, "58956db217c6443e49c91bdd54d7cf76b4a243f225c748b7bf746459fc27bc7d"),
            ModelFileSpec("tokenizer.json", ModelFileRoles.TOKENIZER, 3_562_272, "3f04e34bea22f9144d1a19151154095bc9ce0430bf421304f5797e716288a906"),
            ModelFileSpec("tokenizer_config.json", ModelFileRoles.CONFIG, 414, "0d637373c70a54c3c7202c0c12b40ff4f346c329960283f5a88031717d73c66f"),
            ModelFileSpec("config.json", ModelFileRoles.CONFIG, 1_188, "6b52ed838c3bc347ef519659d82fbd35b9ee805fc7a6da92fc46bc58dfa08770"),
        ),
        source = ModelSource(
            url = "https://huggingface.co/$chatterboxRepository/tree/$chatterboxRevision",
            revision = chatterboxRevision,
            licenseName = "MIT",
            attribution = "Chatterbox Turbo by Resemble AI. Generated audio is not watermarked in this release.",
        ),
        languages = linkedSetOf("English"),
        supportedLanguageCount = 1,
        sampleRateHz = 24_000,
        speakerCount = null,
        ttsVoiceMode = TtsVoiceMode.REFERENCE_AUDIO,
        ttsControls = setOf(
            TtsControl.LANGUAGE,
            TtsControl.REFERENCE_VOICE,
            TtsControl.EXPRESSIVE_TAGS,
        ),
        // Full synthesis memory is intentionally unset until the S24+ approval run.
        approximateRamBytes = null,
        catalogVersion = CatalogDefaults.VERSION,
        installedAtEpochMs = 0,
    ),
    state = ModelCatalogState.PROVISIONAL,
    download = CatalogDownload(
        expectedBytes = 724_921_110,
        files = chatterboxFiles(
            "conditional_decoder_q4.onnx",
            "conditional_decoder_q4.onnx_data",
            "embed_tokens_q4.onnx",
            "embed_tokens_q4.onnx_data",
            "language_model_q4.onnx",
            "language_model_q4.onnx_data",
            "speech_encoder_q4.onnx",
            "speech_encoder_q4.onnx_data",
            "tokenizer.json",
            "tokenizer_config.json",
            "config.json",
        ),
    ),
)

private fun chatterboxFiles(vararg relativePaths: String): List<CatalogDownloadFile> = relativePaths.map { relativePath ->
    CatalogDownloadFile(
        relativePath = relativePath,
        url = "https://huggingface.co/$chatterboxRepository/resolve/$chatterboxRevision/" +
            if (relativePath.endsWith(".onnx") || relativePath.endsWith(".onnx_data")) {
                "onnx/$relativePath"
            } else {
                relativePath
            },
    )
}
