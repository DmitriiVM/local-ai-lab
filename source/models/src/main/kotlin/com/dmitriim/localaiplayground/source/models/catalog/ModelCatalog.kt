package com.dmitriim.localaiplayground.source.models.catalog

import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.library.CatalogArchiveFormat
import com.dmitriim.localaiplayground.core.model.library.CatalogDownload
import com.dmitriim.localaiplayground.core.model.library.CatalogDownloadArchive
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
import com.dmitriim.localaiplayground.core.model.manifest.SttRecognitionMode
import com.dmitriim.localaiplayground.core.model.manifest.TtsControl
import com.dmitriim.localaiplayground.core.model.manifest.TtsVoiceDescriptor
import com.dmitriim.localaiplayground.core.model.manifest.TtsVoiceMode

/** Immutable, app-bundled catalog. Remote hosts provide bytes only, never catalog updates. */
internal object ModelCatalog {
    private const val catalogVersion = 3
    private const val apacheAttribution = "Apache License 2.0; source and model attribution are shown in Model details."
    private const val mitAttribution = "MIT licensed upstream model bundle; attribution is shown in Model details."
    private const val whisperTinyRepository = "csukuangfj/sherpa-onnx-whisper-tiny"
    private const val whisperTinyRevision = "65176e2deb88badc814a94058666cadccc29b61c"
    private const val whisperBaseRepository = "csukuangfj/sherpa-onnx-whisper-base"
    private const val whisperBaseRevision = "bb53ee204431c90d314c1cc08d28d23e5b7927cc"
    private const val whisperSmallRepository = "csukuangfj/sherpa-onnx-whisper-small"
    private const val whisperSmallRevision = "8f3c18b358db4d1f2fc1eae49d75cd20989e4309"
    private const val supertonicRepository = "csukuangfj2/sherpa-onnx-supertonic-3-tts-int8-2026-05-11"
    private const val supertonicRevision = "cca5a0e6c96e1d2c720986bf7e75fcc81dee3ae4"
    private const val piperDownloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-lessac-medium.tar.bz2"
    private const val kokoroDownloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2"
    private const val pocketTtsDownloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/sherpa-onnx-pocket-tts-int8-2026-01-26.tar.bz2"
    private const val chatterboxRepository = "ResembleAI/chatterbox-turbo-ONNX"
    private const val chatterboxRevision = "7232a922b8b11a00f473f0a4d2ec233c2148c905"

    val entries: List<CatalogModel> = listOf(
        CatalogModel(
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
                catalogVersion = catalogVersion,
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
        ),
        llmModel(
            modelId = "qwen3-1.7b-q4-k-m",
            displayName = "Qwen3 1.7B Q4_K_M",
            family = "Qwen3",
            description = "A 1.7B multilingual chat model packaged as a Q4_K_M GGUF for local text generation.",
            repository = "ggml-org/Qwen3-1.7B-GGUF",
            revision = "daeb8e2d528a760970442092f6bf1e55c3b659eb",
            fileName = "Qwen3-1.7B-Q4_K_M.gguf",
            quantization = "Q4_K_M",
            expectedBytes = 1_282_439_264,
            sha256 = "d2387ca2dbfee2ffabce7120d3770dadca0b293052bc2f0e138fdc940d9bc7b5",
            languages = linkedSetOf("English", "Russian", "Chinese"),
            supportedLanguageCount = 119,
            approximateRamBytes = 2_300_000_000,
            state = ModelCatalogState.APPROVED,
        ),
        llmModel(
            modelId = "qwen3.5-0.8b-q4-0",
            displayName = "Qwen3.5 0.8B Q4_0",
            family = "Qwen3.5",
            description = "A compact 0.8B English-and-Chinese chat model packaged as a Q4_0 GGUF.",
            repository = "ggml-org/Qwen3.5-0.8B-GGUF",
            revision = "8fea620810c4afa23dd6443f999a48574c1611a3",
            fileName = "Qwen3.5-0.8B-Q4_0.gguf",
            quantization = "Q4_0",
            expectedBytes = 563_036_064,
            sha256 = "57d1997790d1744fba5b40a7317df71ea5e2acee28c47e78f0cce39c0703f8cf",
            languages = linkedSetOf("English", "Chinese"),
            approximateRamBytes = 1_200_000_000,
        ),
        llmModel(
            modelId = "lfm2.5-1.2b-instruct-q4-k-m",
            displayName = "LFM2.5 1.2B Instruct Q4_K_M",
            family = "LFM2.5",
            description = "A 1.2B instruction-tuned chat model covering eight catalogued languages in Q4_K_M format.",
            repository = "LiquidAI/LFM2.5-1.2B-Instruct-GGUF",
            revision = "047e06635fbe71469926b35ea414537245218200",
            fileName = "LFM2.5-1.2B-Instruct-Q4_K_M.gguf",
            quantization = "Q4_K_M",
            expectedBytes = 730_895_168,
            sha256 = "b1b3de114215d9507409a662a501a631095a479a419584e8a2ded6304b19b4f5",
            languages = linkedSetOf("English", "Arabic", "Chinese", "French", "German", "Japanese", "Korean", "Spanish"),
            supportedLanguageCount = 8,
            approximateRamBytes = 1_500_000_000,
            licenseName = "LFM Open License v1.0",
            attribution = "LFM2.5 by Liquid AI; use is subject to the LFM Open License v1.0.",
        ),
        llmModel(
            modelId = "llama-3.2-1b-instruct-q4-k-m",
            displayName = "Llama 3.2 1B Instruct Q4_K_M",
            family = "Llama 3.2",
            description = "A compact 1B instruction-tuned chat model covering eight catalogued languages.",
            repository = "bartowski/Llama-3.2-1B-Instruct-GGUF",
            revision = "067b946cf014b7c697f3654f621d577a3e3afd1c",
            fileName = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            quantization = "Q4_K_M",
            expectedBytes = 807_694_464,
            sha256 = "6f85a640a97cf2bf5b8e764087b1e83da0fdb51d7c9fab7d0fece9385611df83",
            languages = linkedSetOf("English", "German", "French", "Italian", "Portuguese", "Hindi", "Spanish", "Thai"),
            supportedLanguageCount = 8,
            approximateRamBytes = 1_500_000_000,
            licenseName = "Llama 3.2 Community License",
            attribution = "Llama 3.2 by Meta; quantized GGUF by bartowski. The Llama 3.2 Community License applies.",
        ),
        llmModel(
            modelId = "gemma-3-1b-it-q4-k-m",
            displayName = "Gemma 3 1B IT Q4_K_M",
            family = "Gemma 3",
            description = "A compact 1B instruction-tuned multilingual chat model packaged as a Q4_K_M GGUF.",
            repository = "ggml-org/gemma-3-1b-it-GGUF",
            revision = "f9c28bcd85737ffc5aef028638d3341d49869c27",
            fileName = "gemma-3-1b-it-Q4_K_M.gguf",
            quantization = "Q4_K_M",
            expectedBytes = 806_058_240,
            sha256 = "8ccc5cd1f1b3602548715ae25a66ed73fd5dc68a210412eea643eb20eb75a135",
            languages = linkedSetOf("English", "Russian", "Chinese"),
            supportedLanguageCount = 140,
            approximateRamBytes = 1_600_000_000,
            licenseName = "Gemma Terms of Use",
            attribution = "Gemma 3 by Google; use is subject to the Gemma Terms of Use and Prohibited Use Policy.",
        ),
        llmModel(
            modelId = "deepseek-r1-distill-qwen-1.5b-q4-k-m",
            displayName = "DeepSeek R1 Distill Qwen 1.5B Q4_K_M",
            family = "DeepSeek R1 Distill Qwen",
            description = "A 1.5B distilled English-and-Chinese chat model packaged as a Q4_K_M GGUF.",
            repository = "bartowski/DeepSeek-R1-Distill-Qwen-1.5B-GGUF",
            revision = "9cc28b17e86fa2415fcb070f8ee5ec27c965aa61",
            fileName = "DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
            quantization = "Q4_K_M",
            expectedBytes = 1_117_320_800,
            sha256 = "1741e5b2d062b07acf048bf0d2c514dadf2a48f94e2b4aa0cfe069af3838ee2f",
            languages = linkedSetOf("English", "Chinese"),
            supportedLanguageCount = 2,
            approximateRamBytes = 2_100_000_000,
            licenseName = "MIT",
            attribution = "DeepSeek R1 Distill Qwen by DeepSeek; quantized GGUF by bartowski. MIT licensed.",
        ),
        llmModel(
            modelId = "smollm3-3b-q4-k-m",
            displayName = "SmolLM3 3B Q4_K_M",
            family = "SmolLM3",
            description = "A 3B multilingual chat model covering six catalogued languages in Q4_K_M format.",
            repository = "ggml-org/SmolLM3-3B-GGUF",
            revision = "4965cb60b150737b68a0408c36aeefb65078f894",
            fileName = "SmolLM3-Q4_K_M.gguf",
            quantization = "Q4_K_M",
            expectedBytes = 1_915_305_312,
            sha256 = "8334b850b7bd46238c16b0c550df2138f0889bf433809008cc17a8b05761863e",
            languages = linkedSetOf("English", "French", "Spanish", "German", "Italian", "Portuguese"),
            supportedLanguageCount = 6,
            approximateRamBytes = 3_200_000_000,
        ),
        llmModel(
            modelId = "llama-3.2-3b-instruct-q4-k-m",
            displayName = "Llama 3.2 3B Instruct Q4_K_M",
            family = "Llama 3.2",
            description = "A 3B instruction-tuned chat model covering eight catalogued languages in Q4_K_M format.",
            repository = "bartowski/Llama-3.2-3B-Instruct-GGUF",
            revision = "5ab33fa94d1d04e903623ae72c95d1696f09f9e8",
            fileName = "Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            quantization = "Q4_K_M",
            expectedBytes = 2_019_377_696,
            sha256 = "6c1a2b41161032677be168d354123594c0e6e67d2b9227c84f296ad037c728ff",
            languages = linkedSetOf("English", "German", "French", "Italian", "Portuguese", "Hindi", "Spanish", "Thai"),
            supportedLanguageCount = 8,
            approximateRamBytes = 3_400_000_000,
            licenseName = "Llama 3.2 Community License",
            attribution = "Llama 3.2 by Meta; quantized GGUF by bartowski. The Llama 3.2 Community License applies.",
        ),
        llmModel(
            modelId = "phi-4-mini-instruct-q4-k-m",
            displayName = "Phi-4 Mini Instruct Q4_K_M",
            family = "Phi-4 Mini",
            description = "A multilingual instruction-tuned chat model packaged as a Q4_K_M GGUF.",
            repository = "unsloth/Phi-4-mini-instruct-GGUF",
            revision = "78eb92a46fc37e6b524df991ed9aca9bc6aa7b80",
            fileName = "Phi-4-mini-instruct-Q4_K_M.gguf",
            quantization = "Q4_K_M",
            expectedBytes = 2_491_874_272,
            sha256 = "88c00229914083cd112853aab84ed51b87bdf6b9ce42f532d8c85c7c63b1730a",
            languages = linkedSetOf("English", "Chinese", "French", "German", "Russian", "Spanish"),
            supportedLanguageCount = 23,
            approximateRamBytes = 4_000_000_000,
            licenseName = "MIT",
            attribution = "Phi-4 Mini by Microsoft; quantized GGUF by Unsloth. MIT licensed.",
        ),
        CatalogModel(
            manifest = ModelManifest(
                modelId = ModelId("pocket-tts-int8-en-2026-01-26"),
                displayName = "Pocket TTS INT8 (English)",
                family = "Pocket TTS",
                description = "An English reference-voice text-to-speech model with bundled reference audio.",
                capabilities = setOf(AiCapability.TEXT_TO_SPEECH),
                engineId = EngineId("sherpa-onnx"),
                profileType = ModelProfileIds.POCKET_TTS,
                format = ModelFormat.ONNX,
                quantization = "INT8",
                architecture = "Pocket TTS",
                revision = "2026-01-26",
                files = listOf(
                    ModelFileSpec("lm_flow.int8.onnx", ModelFileRoles.LM_FLOW, 9_962_530, "8d627d235c44a597da908e1085ebe241cbbe358964c502c5a5063d18851a5529"),
                    ModelFileSpec("lm_main.int8.onnx", ModelFileRoles.LM_MAIN, 76_341_079, "bfc0c7e7e3d72864fa3bb2ee499f62f21ddc1474b885f5f3ca570f8be73e787e"),
                    ModelFileSpec("encoder.onnx", ModelFileRoles.POCKET_ENCODER, 72_713_165, "e8f2f6d301ffb96e398b138a7dc6d3038622d236044636b73d920bab85890260"),
                    ModelFileSpec("decoder.int8.onnx", ModelFileRoles.POCKET_DECODER, 22_693_618, "12b0857402d31aead94df19d6783b4350d1f740e811f3a3202c70ad89ae11eea"),
                    ModelFileSpec("text_conditioner.onnx", ModelFileRoles.TEXT_CONDITIONER, 16_388_343, "0b84e837d7bfaf2c896627b03e3f080320309f37f4fc7df7698c644f7ba5e6b1"),
                    ModelFileSpec("vocab.json", ModelFileRoles.VOCABULARY, 69_478, "6fb646346cf931016f70c4921aab0900ce7a304b893cb02135c74e294abfea01"),
                    ModelFileSpec("token_scores.json", ModelFileRoles.TOKEN_SCORES, 123_616, "5be2f278caf9b9800741f0fd82bff677f4943ec764c356f907213434b622d958"),
                    ModelFileSpec("test_wavs/bria.wav", ModelFileRoles.REFERENCE_AUDIO, 2_152_986, "85f46d6f0642f657a6bd689ddaa52d5a5f53e4314715e1032704c80917392181"),
                ),
                source = ModelSource(
                    url = pocketTtsDownloadUrl,
                    revision = "2026-01-26",
                    licenseName = "CC-BY-4.0",
                    attribution = "Pocket TTS by Kyutai; ONNX package distributed by sherpa-onnx.",
                ),
                languages = linkedSetOf("English"),
                supportedLanguageCount = 1,
                sampleRateHz = 24_000,
                speakerCount = 1,
                voices = listOf(
                    ttsVoice(
                        id = "bundled-reference",
                        displayName = "Bundled reference",
                        speakerId = 0,
                        description = "English · Bundled reference",
                        languages = arrayOf("en"),
                    ),
                ),
                approximateRamBytes = 350_000_000,
                catalogVersion = catalogVersion,
                installedAtEpochMs = 0,
            ),
            state = ModelCatalogState.APPROVED,
            download = CatalogDownload(
                expectedBytes = 98_336_520,
                archive = CatalogDownloadArchive(
                    url = pocketTtsDownloadUrl,
                    expectedBytes = 98_336_520,
                    sha256 = "2f3b88823cbbb9bf0b2477ec8ae7b3fec417b3a87b6bb5f256dba66f2ad967cb",
                    rootDirectory = "sherpa-onnx-pocket-tts-int8-2026-01-26",
                ),
            ),
        ),
        CatalogModel(
            manifest = ModelManifest(
                modelId = ModelId("kokoro-multi-lang-v1-0"),
                displayName = "Kokoro Multi-Lang v1.0",
                family = "Kokoro",
                description = "An English-and-Chinese text-to-speech model with 53 bundled voices.",
                capabilities = setOf(AiCapability.TEXT_TO_SPEECH),
                engineId = EngineId("sherpa-onnx"),
                profileType = ModelProfileIds.KOKORO_TTS,
                format = ModelFormat.ONNX,
                revision = "tts-models",
                files = listOf(
                    ModelFileSpec("model.onnx", ModelFileRoles.KOKORO_MODEL, 325_630_829, "c436dc6a842b62aba06af67e40bafcfb9c60ac3af895358f1974ad9a7f7c026b"),
                    ModelFileSpec("voices.bin", ModelFileRoles.VOICE_EMBEDDINGS, 27_678_720, "8a77c0d397026208d22211f37670b5b3b11e03f190756b25a1d24041fced82a9"),
                    ModelFileSpec("tokens.txt", ModelFileRoles.TOKENS, 687, "6ebb6bb288f20f3ae8d004d3c2ca27697da27c037d75e81a60e2a6a663f95425"),
                    ModelFileSpec("lexicon-us-en.txt", ModelFileRoles.LEXICON, 5_956_885, "7daaab53a181be9885b853a8582bf1838186317e5dadacbcef9c426d6fa0da14"),
                    ModelFileSpec("lexicon-zh.txt", ModelFileRoles.LEXICON, 2_364_621, "509a1f55bf9c62e3f7e598e7544b114eadef1e00266f2badff4f281153f9f327"),
                    ModelFileSpec("date-zh.fst", ModelFileRoles.TEXT_RULES, 59_154, "eb8aa079ae3cb81d8f4404992f39d61a0cb990947512b5b8d1e54d1f6980e718"),
                    ModelFileSpec("number-zh.fst", ModelFileRoles.TEXT_RULES, 64_482, "743f402181fcfebf76cc2f0546b71fa26476e626fbe4e460fb7b4c3a7a8bd5bd"),
                    ModelFileSpec("phone-zh.fst", ModelFileRoles.TEXT_RULES, 88_630, "1ac2b6fa56b1442320c4de7db08353bab8963a2b57f365eebcdd3a2d3562f8d7"),
                    ModelFileSpec("espeak-ng-data", ModelFileRoles.FRONTEND_DATA, directory = true),
                    ModelFileSpec("dict", ModelFileRoles.DICTIONARY_DATA, directory = true),
                ),
                source = ModelSource(
                    url = kokoroDownloadUrl,
                    revision = "tts-models",
                    licenseName = "Apache-2.0",
                    attribution = "Kokoro Multi-Lang v1.0 model package distributed by sherpa-onnx.",
                ),
                languages = linkedSetOf("English", "Chinese"),
                supportedLanguageCount = 2,
                sampleRateHz = 24_000,
                speakerCount = 53,
                voices = kokoroV1Voices(),
                approximateRamBytes = 900_000_000,
                catalogVersion = catalogVersion,
                installedAtEpochMs = 0,
            ),
            state = ModelCatalogState.APPROVED,
            download = CatalogDownload(
                expectedBytes = 349_418_188,
                archive = CatalogDownloadArchive(
                    url = kokoroDownloadUrl,
                    expectedBytes = 349_418_188,
                    sha256 = "c133d26353d776da730870dac7da07dbfc9a5e3bc80cc5e8e83ab6e823be7046",
                    rootDirectory = "kokoro-multi-lang-v1_0",
                ),
            ),
        ),
        whisperModel(
            modelId = "whisper-tiny-int8",
            displayName = "Whisper Tiny INT8",
            description = "A multilingual Whisper Tiny speech-to-text model quantized to INT8 for offline transcription.",
            repository = whisperTinyRepository,
            revision = whisperTinyRevision,
            filePrefix = "tiny",
            encoderBytes = 12_937_772,
            encoderSha256 = "d24fb083ae3b1041fc24e97971d60e280c9342201fbb67b0ab428a8b4a51a434",
            decoderBytes = 89_855_401,
            decoderSha256 = "d2fece8dd42771f1df975c6c0445770d0c292bf7547c2cae04a6c0cc57540925",
            approximateRamBytes = 500_000_000,
        ),
        whisperModel(
            modelId = "whisper-base-int8",
            displayName = "Whisper Base INT8",
            description = "A multilingual Whisper Base speech-to-text model quantized to INT8 for offline transcription.",
            repository = whisperBaseRepository,
            revision = whisperBaseRevision,
            filePrefix = "base",
            encoderBytes = 29_120_534,
            encoderSha256 = "0b8fb1304b6109976038efff5ace81720e00386f3ff6b54ee8c75291ca0a1e11",
            decoderBytes = 130_672_026,
            decoderSha256 = "9759d217388a01b3a4c7c15533201067b48ae819c4daafc8624e64b9409dc02d",
            approximateRamBytes = 850_000_000,
        ),
        whisperModel(
            modelId = "whisper-small-int8",
            displayName = "Whisper Small INT8",
            description = "A multilingual Whisper Small speech-to-text model quantized to INT8 for offline transcription.",
            repository = whisperSmallRepository,
            revision = whisperSmallRevision,
            filePrefix = "small",
            encoderBytes = 112_442_483,
            encoderSha256 = "4cbe7b22fa9026b843b60a68640c747de05bafb1a11b57edc0e66c232d9f33a9",
            decoderBytes = 262_226_114,
            decoderSha256 = "acad50b5c782696e91b55914cc5ab4f756f1532f76e22aa6fc615f39fb69a8ee",
            approximateRamBytes = 2_600_000_000,
        ),
        sttArchiveModel(
            modelId = "parakeet-tdt-ctc-110m-en-int8",
            displayName = "Parakeet TDT-CTC 110M INT8",
            family = "Parakeet",
            description = "An English offline speech-to-text model quantized to INT8.",
            profileType = ModelProfileIds.PARAKEET_CTC_STT,
            archiveName = "sherpa-onnx-nemo-parakeet_tdt_ctc_110m-en-36000-int8",
            archiveBytes = 104_337_827,
            archiveSha256 = "17f945007b52ccd8b7200ffc7c5652e9e8e961dfdf479cefcabd06cf5703630b",
            files = listOf(
                ModelFileSpec("model.int8.onnx", ModelFileRoles.PRIMARY_MODEL, expectedBytes = 131_652_171),
                ModelFileSpec("tokens.txt", ModelFileRoles.TOKENS, expectedBytes = 9_953),
            ),
            languages = linkedSetOf("English"),
            licenseName = "CC-BY-4.0",
            attribution = "NVIDIA Parakeet TDT-CTC 110M, converted and packaged for sherpa-onnx.",
            approximateRamBytes = 500_000_000,
        ),
        sttArchiveModel(
            modelId = "gigaam-v2-ctc-ru-int8",
            displayName = "GigaAM v2 CTC INT8",
            family = "GigaAM",
            description = "A Russian offline speech-to-text model quantized to INT8.",
            profileType = ModelProfileIds.GIGAAM_CTC_STT,
            archiveName = "sherpa-onnx-nemo-ctc-giga-am-v2-russian-2025-04-19",
            archiveBytes = 166_917_722,
            archiveSha256 = "777be8717d8aaf04861823671290f7687f7579fd9ac63a2124955573f920caf5",
            files = listOf(
                ModelFileSpec("model.int8.onnx", ModelFileRoles.PRIMARY_MODEL, expectedBytes = 236_457_977),
                ModelFileSpec("tokens.txt", ModelFileRoles.TOKENS, expectedBytes = 196),
            ),
            languages = linkedSetOf("Russian"),
            licenseName = "MIT",
            attribution = "GigaAM v2 by SberDevices, converted and packaged for sherpa-onnx.",
            approximateRamBytes = 800_000_000,
        ),
        sttArchiveModel(
            modelId = "zipformer-en-20m-streaming-int8",
            displayName = "Zipformer 20M Streaming INT8",
            family = "Zipformer",
            description = "An English streaming speech-to-text model quantized to INT8.",
            profileType = ModelProfileIds.ZIPFORMER_STT,
            archiveName = "sherpa-onnx-streaming-zipformer-en-20M-2023-02-17",
            archiveBytes = 127_887_156,
            archiveSha256 = "9c559283e8498d3fe95913c79ca1cb454bb26281ac2b102b41306c7d752765d9",
            files = listOf(
                ModelFileSpec("encoder-epoch-99-avg-1.int8.onnx", ModelFileRoles.ENCODER, expectedBytes = 42_845_182),
                ModelFileSpec("decoder-epoch-99-avg-1.int8.onnx", ModelFileRoles.DECODER, expectedBytes = 539_499),
                ModelFileSpec("joiner-epoch-99-avg-1.int8.onnx", ModelFileRoles.JOINER, expectedBytes = 259_572),
                ModelFileSpec("tokens.txt", ModelFileRoles.TOKENS, expectedBytes = 5_048),
            ),
            languages = linkedSetOf("English"),
            licenseName = "Apache-2.0",
            attribution = "Icefall streaming Zipformer model packaged for sherpa-onnx.",
            recognitionMode = SttRecognitionMode.STREAMING,
            approximateRamBytes = 350_000_000,
        ),
        sttArchiveModel(
            modelId = "sensevoice-small-5lang-int8",
            displayName = "SenseVoice Small INT8",
            family = "SenseVoice",
            description = "An offline speech-to-text model supporting Chinese, English, Japanese, Korean, and Cantonese.",
            profileType = ModelProfileIds.SENSE_VOICE_STT,
            archiveName = "sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2024-07-17",
            archiveBytes = 163_002_883,
            archiveSha256 = "7d1efa2138a65b0b488df37f8b89e3d91a60676e416f515b952358d83dfd347e",
            files = listOf(
                ModelFileSpec("model.int8.onnx", ModelFileRoles.PRIMARY_MODEL),
                ModelFileSpec("tokens.txt", ModelFileRoles.TOKENS),
            ),
            languages = linkedSetOf("Chinese", "English", "Japanese", "Korean", "Cantonese"),
            licenseName = "See upstream model license",
            attribution = "SenseVoiceSmall by FunAudioLLM, converted and packaged for sherpa-onnx.",
            approximateRamBytes = 700_000_000,
        ),
        sttArchiveModel(
            modelId = "paraformer-zh-en-small-int8",
            displayName = "Paraformer Small INT8",
            family = "Paraformer",
            description = "A Chinese-and-English offline speech-to-text model quantized to INT8.",
            profileType = ModelProfileIds.PARAFORMER_STT,
            archiveName = "sherpa-onnx-paraformer-zh-small-2024-03-09",
            archiveBytes = 77_920_048,
            archiveSha256 = "da92b3db5218c5be53aad53e57d1b6e63e7fc98a0e054fbdd6dbe18e9c6b1450",
            files = listOf(
                ModelFileSpec("model.int8.onnx", ModelFileRoles.PRIMARY_MODEL, expectedBytes = 81_828_675),
                ModelFileSpec("tokens.txt", ModelFileRoles.TOKENS, expectedBytes = 75_352),
            ),
            languages = linkedSetOf("Chinese", "English"),
            licenseName = "Apache-2.0",
            attribution = "Paraformer model from ModelScope, converted and packaged for sherpa-onnx.",
            approximateRamBytes = 350_000_000,
        ),
        sttArchiveModel(
            modelId = "moonshine-base-en-quantized",
            displayName = "Moonshine v2 Base Quantized",
            family = "Moonshine",
            description = "A quantized English offline speech-to-text model.",
            profileType = ModelProfileIds.MOONSHINE_STT,
            archiveName = "sherpa-onnx-moonshine-base-en-quantized-2026-02-27",
            archiveBytes = 111_266_225,
            archiveSha256 = "43232c1d13013d37317163baec3135bd771a186a4356f28c889bab453bb0e891",
            files = listOf(
                ModelFileSpec("encoder_model.ort", ModelFileRoles.ENCODER, expectedBytes = 31_326_816),
                ModelFileSpec("decoder_model_merged.ort", ModelFileRoles.MERGED_DECODER, expectedBytes = 109_424_400),
                ModelFileSpec("tokens.txt", ModelFileRoles.TOKENS, expectedBytes = 549_350),
            ),
            languages = linkedSetOf("English"),
            licenseName = "CC-BY-4.0",
            attribution = "Moonshine v2 by Useful Sensors, quantized and packaged for sherpa-onnx.",
            approximateRamBytes = 550_000_000,
        ),
        sttArchiveModel(
            modelId = "vosk-small-en-us-0-15",
            displayName = "Vosk Small English US",
            family = "Vosk",
            description = "A lightweight streaming speech-to-text model for US English.",
            engineId = EngineId("vosk"),
            profileType = ModelProfileIds.VOSK_STT,
            archiveName = "vosk-model-small-en-us-0.15",
            downloadUrl = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip",
            archiveBytes = 41_205_931,
            archiveSha256 = "30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498",
            archiveFormat = CatalogArchiveFormat.ZIP,
            files = voskDirectories(),
            languages = linkedSetOf("English"),
            licenseName = "Apache-2.0",
            attribution = "Vosk lightweight US English model by Alpha Cephei.",
            recognitionMode = SttRecognitionMode.STREAMING,
            quantization = null,
            approximateRamBytes = 300_000_000,
        ),
        sttArchiveModel(
            modelId = "vosk-small-ru-0-22",
            displayName = "Vosk Small Russian",
            family = "Vosk",
            description = "A lightweight streaming speech-to-text model for Russian.",
            engineId = EngineId("vosk"),
            profileType = ModelProfileIds.VOSK_STT,
            archiveName = "vosk-model-small-ru-0.22",
            downloadUrl = "https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip",
            archiveBytes = 46_236_750,
            archiveSha256 = "961d5ff98a17f4aa6de69864d0aa71fa5bac682301d2b5d17a3f24c5c99a46d4",
            archiveFormat = CatalogArchiveFormat.ZIP,
            files = voskDirectories(),
            languages = linkedSetOf("Russian"),
            licenseName = "Apache-2.0",
            attribution = "Vosk lightweight Russian model by Alpha Cephei.",
            recognitionMode = SttRecognitionMode.STREAMING,
            quantization = null,
            approximateRamBytes = 300_000_000,
        ),
        CatalogModel(
            manifest = ModelManifest(
                modelId = ModelId("supertonic-3-int8"), displayName = "Supertonic 3 INT8", family = "Supertonic",
                description = "A multilingual text-to-speech model with 10 bundled voices.",
                capabilities = setOf(AiCapability.TEXT_TO_SPEECH),
                engineId = EngineId("sherpa-onnx"), profileType = ModelProfileIds.SUPERTONIC_TTS,
                format = ModelFormat.ONNX, quantization = "INT8",
                revision = supertonicRevision,
                files = listOf(
                    ModelFileSpec(
                        "duration_predictor.int8.onnx",
                        ModelFileRoles.DURATION_PREDICTOR,
                        expectedBytes = 3_700_147,
                        sha256 = "c3eb91414d5ff8a7a239b7fe9e34e7e2bf8a8140d8375ffb14718b1c639325db",
                    ),
                    ModelFileSpec(
                        "text_encoder.int8.onnx",
                        ModelFileRoles.TEXT_ENCODER,
                        expectedBytes = 36_416_150,
                        sha256 = "c7befd5ea8c3119769e8a6c1486c4edc6a3bc8365c67621c881bbb774b9902ff",
                    ),
                    ModelFileSpec(
                        "vector_estimator.int8.onnx",
                        ModelFileRoles.VECTOR_ESTIMATOR,
                        expectedBytes = 78_400_833,
                        sha256 = "20cd86fa5c6effedfda0e7cffe5b0569ca401c440a0c3a1d72bf39286c0db3fd",
                    ),
                    ModelFileSpec(
                        "vocoder.int8.onnx",
                        ModelFileRoles.VOCODER,
                        expectedBytes = 25_991_073,
                        sha256 = "e923d60f53f95eb1ce235f1dc33ec56d9c057823c96fa6f8acf98f32b0da6152",
                    ),
                    ModelFileSpec(
                        "tts.json",
                        ModelFileRoles.CONFIG,
                        expectedBytes = 8_253,
                        sha256 = "42078d3aef1cd43ab43021f3c54f47d2d75ceb4e75f627f118890128b06a0d09",
                    ),
                    ModelFileSpec(
                        "unicode_indexer.bin",
                        ModelFileRoles.UNICODE_INDEXER,
                        expectedBytes = 262_144,
                        sha256 = "8402ca48e5189a8950138580b0fff64db6f072f24ac07cd54ba8b2fbb9883b30",
                    ),
                    ModelFileSpec(
                        "voice.bin",
                        ModelFileRoles.VOICE_STYLE,
                        expectedBytes = 517_168,
                        sha256 = "67d5209b0ee8ce6c74105ffbe12fe6a7628aea3b4ba2fcb308a4a67938a93ce8",
                    ),
                ),
                source = ModelSource(
                    url = "https://huggingface.co/$supertonicRepository/tree/$supertonicRevision",
                    revision = supertonicRevision,
                    licenseName = "MIT",
                    attribution = mitAttribution,
                ),
                languages = linkedSetOf("English", "Russian", "German"), supportedLanguageCount = 31, sampleRateHz = 44_100,
                speakerCount = 10,
                voices = supertonicV3Voices(),
                approximateRamBytes = 900_000_000, catalogVersion = catalogVersion, installedAtEpochMs = 0,
            ),
            state = ModelCatalogState.APPROVED,
            download = CatalogDownload(
                expectedBytes = 145_295_768,
                files = huggingFaceFiles(
                    supertonicRepository,
                    supertonicRevision,
                    "duration_predictor.int8.onnx",
                    "text_encoder.int8.onnx",
                    "vector_estimator.int8.onnx",
                    "vocoder.int8.onnx",
                    "tts.json",
                    "unicode_indexer.bin",
                    "voice.bin",
                ),
            ),
        ),
        CatalogModel(
            manifest = ModelManifest(
                modelId = ModelId("piper-en-us-lessac-medium"),
                displayName = "Piper Lessac Medium (English)",
                family = "Piper/VITS",
                description = "A single-speaker English text-to-speech model using the Lessac voice.",
                capabilities = setOf(AiCapability.TEXT_TO_SPEECH),
                engineId = EngineId("sherpa-onnx"),
                profileType = ModelProfileIds.PIPER_VITS_TTS,
                format = ModelFormat.ONNX,
                revision = "tts-models",
                files = listOf(
                    ModelFileSpec(
                        "en_US-lessac-medium.onnx",
                        ModelFileRoles.VITS_MODEL,
                        expectedBytes = 63_149_198,
                        sha256 = "4ba07d8549906668ee855fd9abf9faf66c5db74742712ff026a159f7277fca9f",
                    ),
                    ModelFileSpec(
                        "tokens.txt",
                        ModelFileRoles.TOKENS,
                        expectedBytes = 921,
                        sha256 = "87c8ef66eae5473ed0cc0366b3964c736ca6c5f676c979522ea31234e47430b9",
                    ),
                    ModelFileSpec("espeak-ng-data", ModelFileRoles.FRONTEND_DATA, directory = true),
                ),
                source = ModelSource(
                    url = piperDownloadUrl,
                    revision = "tts-models",
                    licenseName = "Upstream Piper model terms",
                    attribution = "Piper Lessac Medium model package distributed by sherpa-onnx.",
                ),
                languages = linkedSetOf("English"),
                supportedLanguageCount = 1,
                sampleRateHz = 22_050,
                speakerCount = 1,
                voices = listOf(
                    ttsVoice(
                        id = "lessac",
                        displayName = "Lessac",
                        speakerId = 0,
                        description = "English (United States) · Medium quality",
                        languages = arrayOf("en"),
                    ),
                ),
                approximateRamBytes = 160_000_000,
                catalogVersion = catalogVersion,
                installedAtEpochMs = 0,
            ),
            state = ModelCatalogState.APPROVED,
            download = CatalogDownload(
                expectedBytes = 67_230_653,
                archive = CatalogDownloadArchive(
                    url = piperDownloadUrl,
                    expectedBytes = 67_230_653,
                    sha256 = "9e3febfacf0abf4270172d2958bcec246032b7e88efc2720840cc80c93de334e",
                    rootDirectory = "vits-piper-en_US-lessac-medium",
                ),
            ),
        ),
    )

    private fun llmModel(
        modelId: String,
        displayName: String,
        family: String,
        description: String,
        repository: String,
        revision: String,
        fileName: String,
        quantization: String,
        expectedBytes: Long,
        sha256: String,
        languages: Set<String>,
        approximateRamBytes: Long,
        supportedLanguageCount: Int? = null,
        licenseName: String = "Apache-2.0",
        attribution: String = apacheAttribution,
        state: ModelCatalogState = ModelCatalogState.OPTIONAL,
    ): CatalogModel {
        val downloadUrl = "https://huggingface.co/$repository/resolve/$revision/$fileName"
        val sourceUrl = "https://huggingface.co/$repository/tree/$revision"
        return CatalogModel(
            manifest = ModelManifest(
                modelId = ModelId(modelId),
                displayName = displayName,
                family = family,
                description = description,
                capabilities = setOf(AiCapability.CHAT),
                engineId = EngineId("llama.cpp"),
                profileType = ModelProfileIds.LLM,
                format = ModelFormat.GGUF,
                quantization = quantization,
                architecture = family,
                revision = revision,
                files = listOf(
                    ModelFileSpec(
                        relativePath = fileName,
                        role = ModelFileRoles.PRIMARY_MODEL,
                        expectedBytes = expectedBytes,
                        sha256 = sha256,
                    ),
                ),
                source = ModelSource(
                    url = sourceUrl,
                    revision = revision,
                    licenseName = licenseName,
                    attribution = attribution,
                ),
                languages = languages,
                supportedLanguageCount = supportedLanguageCount,
                contextSize = 512,
                approximateRamBytes = approximateRamBytes,
                catalogVersion = catalogVersion,
                installedAtEpochMs = 0,
            ),
            state = state,
            download = CatalogDownload(
                url = downloadUrl,
                expectedBytes = expectedBytes,
                sha256 = sha256,
            ),
        )
    }

    private fun whisperModel(
        modelId: String,
        displayName: String,
        description: String,
        repository: String,
        revision: String,
        filePrefix: String,
        encoderBytes: Long,
        encoderSha256: String,
        decoderBytes: Long,
        decoderSha256: String,
        approximateRamBytes: Long,
    ): CatalogModel {
        val encoder = "$filePrefix-encoder.int8.onnx"
        val decoder = "$filePrefix-decoder.int8.onnx"
        val tokens = "$filePrefix-tokens.txt"
        val tokenBytes = 816_730L
        return CatalogModel(
            manifest = ModelManifest(
                modelId = ModelId(modelId),
                displayName = displayName,
                family = "Whisper",
                description = description,
                capabilities = setOf(AiCapability.SPEECH_TO_TEXT),
                engineId = EngineId("sherpa-onnx"),
                profileType = ModelProfileIds.WHISPER_STT,
                format = ModelFormat.ONNX,
                quantization = "INT8",
                architecture = displayName.removeSuffix(" INT8"),
                revision = revision,
                files = listOf(
                    ModelFileSpec(encoder, ModelFileRoles.ENCODER, encoderBytes, encoderSha256),
                    ModelFileSpec(decoder, ModelFileRoles.DECODER, decoderBytes, decoderSha256),
                    ModelFileSpec(
                        tokens,
                        ModelFileRoles.TOKENS,
                        tokenBytes,
                        "b34b360dbb493e781e479794586d661700670d65564001f23024971d1f2fa126",
                    ),
                ),
                source = ModelSource(
                    url = "https://huggingface.co/$repository/tree/$revision",
                    revision = revision,
                    licenseName = "Apache-2.0",
                    attribution = apacheAttribution,
                ),
                languages = linkedSetOf("English", "Russian", "Spanish"),
                supportedLanguageCount = 99,
                sampleRateHz = 16_000,
                approximateRamBytes = approximateRamBytes,
                catalogVersion = catalogVersion,
                installedAtEpochMs = 0,
            ),
            state = ModelCatalogState.APPROVED,
            download = CatalogDownload(
                expectedBytes = encoderBytes + decoderBytes + tokenBytes,
                files = huggingFaceFiles(repository, revision, encoder, decoder, tokens),
            ),
        )
    }

    private fun sttArchiveModel(
        modelId: String,
        displayName: String,
        family: String,
        description: String,
        profileType: com.dmitriim.localaiplayground.core.model.manifest.ModelProfileId,
        archiveName: String,
        archiveBytes: Long,
        archiveSha256: String,
        files: List<ModelFileSpec>,
        languages: Set<String>,
        licenseName: String,
        attribution: String,
        approximateRamBytes: Long,
        engineId: EngineId = EngineId("sherpa-onnx"),
        downloadUrl: String = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/$archiveName.tar.bz2",
        archiveFormat: CatalogArchiveFormat = CatalogArchiveFormat.TAR_BZIP2,
        recognitionMode: SttRecognitionMode = SttRecognitionMode.OFFLINE,
        quantization: String? = "INT8",
    ) = CatalogModel(
        manifest = ModelManifest(
            modelId = ModelId(modelId),
            displayName = displayName,
            family = family,
            description = description,
            capabilities = setOf(AiCapability.SPEECH_TO_TEXT),
            engineId = engineId,
            profileType = profileType,
            format = if (engineId.value == "vosk") ModelFormat.BINARY else ModelFormat.ONNX,
            quantization = quantization,
            architecture = family,
            revision = archiveName.substringAfterLast('-'),
            files = files,
            source = ModelSource(
                url = downloadUrl,
                revision = archiveName,
                licenseName = licenseName,
                attribution = attribution,
            ),
            languages = languages,
            supportedLanguageCount = languages.size,
            sampleRateHz = 16_000,
            sttRecognitionMode = recognitionMode,
            approximateRamBytes = approximateRamBytes,
            catalogVersion = catalogVersion,
            installedAtEpochMs = 0,
        ),
        state = ModelCatalogState.APPROVED,
        download = CatalogDownload(
            expectedBytes = archiveBytes,
            archive = CatalogDownloadArchive(
                url = downloadUrl,
                expectedBytes = archiveBytes,
                sha256 = archiveSha256,
                rootDirectory = archiveName,
                format = archiveFormat,
            ),
        ),
    )

    private fun voskDirectories() = listOf(
        ModelFileSpec("am", ModelFileRoles.PRIMARY_MODEL, directory = true),
        ModelFileSpec("conf", ModelFileRoles.CONFIG, directory = true),
        ModelFileSpec("graph", ModelFileRoles.VOCABULARY, directory = true),
    )

    private fun huggingFaceFiles(
        repository: String,
        revision: String,
        vararg relativePaths: String,
    ): List<CatalogDownloadFile> = relativePaths.map { relativePath ->
        CatalogDownloadFile(
            relativePath = relativePath,
            url = "https://huggingface.co/$repository/resolve/$revision/$relativePath",
        )
    }

    private fun chatterboxFiles(vararg relativePaths: String): List<CatalogDownloadFile> =
        relativePaths.map { relativePath ->
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

    private fun ttsVoice(
        id: String,
        displayName: String,
        speakerId: Int,
        description: String? = null,
        vararg languages: String,
    ) = TtsVoiceDescriptor(
        id = id,
        displayName = displayName,
        speakerId = speakerId,
        languages = languages.toSet(),
        description = description,
    )

    /**
     * The pinned sherpa-onnx export sorts the official JSON filenames before packing voice.bin.
     * That makes the speaker order F1..F5 followed by M1..M5.
     *
     * https://github.com/k2-fsa/sherpa-onnx/blob/f69171f48e9b43f9eb21061f2de18ef2f58ef661/scripts/supertonic/generate_voices_bin.py
     * https://huggingface.co/Supertone/supertonic-3/tree/main/voice_styles
     */
    private fun supertonicV3Voices() = listOf(
        supertonicVoice("F1", 0),
        supertonicVoice("F2", 1),
        supertonicVoice("F3", 2),
        supertonicVoice("F4", 3),
        supertonicVoice("F5", 4),
        supertonicVoice("M1", 5),
        supertonicVoice("M2", 6),
        supertonicVoice("M3", 7),
        supertonicVoice("M4", 8),
        supertonicVoice("M5", 9),
    )

    private fun supertonicVoice(id: String, speakerId: Int) = ttsVoice(
        id = id,
        displayName = id,
        speakerId = speakerId,
        description = "${if (id.startsWith('F')) "Female" else "Male"} · Multilingual",
        languages = arrayOf("en", "ru", "de"),
    )

    /**
     * Official speaker order for sherpa-onnx kokoro-multi-lang-v1_0.
     * https://k2-fsa.github.io/sherpa/onnx/tts/pretrained_models/kokoro.html
     */
    private fun kokoroV1Voices() = listOf(
        kokoroVoice("af_alloy", 0, "en"),
        kokoroVoice("af_aoede", 1, "en"),
        kokoroVoice("af_bella", 2, "en"),
        kokoroVoice("af_heart", 3, "en"),
        kokoroVoice("af_jessica", 4, "en"),
        kokoroVoice("af_kore", 5, "en"),
        kokoroVoice("af_nicole", 6, "en"),
        kokoroVoice("af_nova", 7, "en"),
        kokoroVoice("af_river", 8, "en"),
        kokoroVoice("af_sarah", 9, "en"),
        kokoroVoice("af_sky", 10, "en"),
        kokoroVoice("am_adam", 11, "en"),
        kokoroVoice("am_echo", 12, "en"),
        kokoroVoice("am_eric", 13, "en"),
        kokoroVoice("am_fenrir", 14, "en"),
        kokoroVoice("am_liam", 15, "en"),
        kokoroVoice("am_michael", 16, "en"),
        kokoroVoice("am_onyx", 17, "en"),
        kokoroVoice("am_puck", 18, "en"),
        kokoroVoice("am_santa", 19, "en"),
        kokoroVoice("bf_alice", 20, "en"),
        kokoroVoice("bf_emma", 21, "en"),
        kokoroVoice("bf_isabella", 22, "en"),
        kokoroVoice("bf_lily", 23, "en"),
        kokoroVoice("bm_daniel", 24, "en"),
        kokoroVoice("bm_fable", 25, "en"),
        kokoroVoice("bm_george", 26, "en"),
        kokoroVoice("bm_lewis", 27, "en"),
        kokoroVoice("ef_dora", 28, "es"),
        kokoroVoice("em_alex", 29, "es"),
        kokoroVoice("ff_siwis", 30, "fr"),
        kokoroVoice("hf_alpha", 31, "hi"),
        kokoroVoice("hf_beta", 32, "hi"),
        kokoroVoice("hm_omega", 33, "hi"),
        kokoroVoice("hm_psi", 34, "hi"),
        kokoroVoice("if_sara", 35, "it"),
        kokoroVoice("im_nicola", 36, "it"),
        kokoroVoice("jf_alpha", 37, "ja"),
        kokoroVoice("jf_gongitsune", 38, "ja"),
        kokoroVoice("jf_nezumi", 39, "ja"),
        kokoroVoice("jf_tebukuro", 40, "ja"),
        kokoroVoice("jm_kumo", 41, "ja"),
        kokoroVoice("pf_dora", 42, "pt"),
        kokoroVoice("pm_alex", 43, "pt"),
        kokoroVoice("pm_santa", 44, "pt"),
        kokoroVoice("zf_xiaobei", 45, "zh"),
        kokoroVoice("zf_xiaoni", 46, "zh"),
        kokoroVoice("zf_xiaoxiao", 47, "zh"),
        kokoroVoice("zf_xiaoyi", 48, "zh"),
        kokoroVoice("zm_yunjian", 49, "zh"),
        kokoroVoice("zm_yunxi", 50, "zh"),
        kokoroVoice("zm_yunxia", 51, "zh"),
        kokoroVoice("zm_yunyang", 52, "zh"),
    )

    private fun kokoroVoice(id: String, speakerId: Int, language: String) = ttsVoice(
        id = id,
        displayName = id.substringAfter('_').replaceFirstChar(Char::uppercase),
        speakerId = speakerId,
        description = kokoroVoiceDescription(id),
        languages = arrayOf(language),
    )

    private fun kokoroVoiceDescription(id: String): String {
        val gender = when (id.getOrNull(1)) {
            'f' -> "Female"
            'm' -> "Male"
            else -> error("Unknown Kokoro voice gender: $id")
        }
        val language = when (id.firstOrNull()) {
            'a' -> "American English"
            'b' -> "British English"
            'e' -> "Spanish"
            'f' -> "French"
            'h' -> "Hindi"
            'i' -> "Italian"
            'j' -> "Japanese"
            'p' -> "Brazilian Portuguese"
            'z' -> "Mandarin Chinese"
            else -> error("Unknown Kokoro voice language: $id")
        }
        val quality = kokoroOverallQuality(id)
        return listOfNotNull(gender, language, quality?.let { "Quality $it" }).joinToString(" · ")
    }

    /**
     * Overall grades published with Kokoro's voice metadata; some voices have no published grade.
     * https://huggingface.co/hexgrad/Kokoro-82M/blob/main/VOICES.md
     */
    private fun kokoroOverallQuality(id: String): String? = when (id) {
        "af_heart" -> "A"
        "af_alloy", "af_nova", "bf_isabella", "jf_gongitsune",
        "hf_alpha", "hf_beta", "hm_omega", "hm_psi", "if_sara", "im_nicola",
        "bm_fable", "bm_george", "jf_tebukuro" -> "C"
        "af_aoede", "af_kore", "af_sarah", "am_fenrir", "am_michael", "am_puck",
        "jf_alpha" -> "C+"
        "af_bella" -> "A-"
        "af_nicole", "bf_emma", "ff_siwis" -> "B-"
        "af_jessica", "af_river", "am_echo", "am_eric", "am_liam", "am_onyx",
        "bf_alice", "bf_lily", "bm_daniel", "zf_xiaobei", "zf_xiaoni", "zf_xiaoxiao",
        "zf_xiaoyi", "zm_yunjian", "zm_yunxi", "zm_yunxia", "zm_yunyang" -> "D"
        "af_sky", "jf_nezumi", "jm_kumo" -> "C-"
        "am_adam" -> "F+"
        "am_santa" -> "D-"
        "bm_lewis" -> "D+"
        else -> null
    }
}
