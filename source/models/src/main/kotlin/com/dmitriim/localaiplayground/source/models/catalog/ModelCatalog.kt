package com.dmitriim.localaiplayground.source.models.catalog

import com.dmitriim.localaiplayground.core.model.AiCapability
import com.dmitriim.localaiplayground.core.model.CatalogDownload
import com.dmitriim.localaiplayground.core.model.CatalogDownloadFile
import com.dmitriim.localaiplayground.core.model.CatalogDownloadArchive
import com.dmitriim.localaiplayground.core.model.CatalogModel
import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.core.model.ModelCatalogState
import com.dmitriim.localaiplayground.core.model.ModelFileRoles
import com.dmitriim.localaiplayground.core.model.ModelFileSpec
import com.dmitriim.localaiplayground.core.model.ModelFormat
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelManifest
import com.dmitriim.localaiplayground.core.model.ModelSource
import com.dmitriim.localaiplayground.core.model.ModelProfileIds

/** Immutable, app-bundled catalog. Remote hosts provide bytes only, never catalog updates. */
internal object ModelCatalog {
    private const val catalogVersion = 1
    private const val apacheAttribution = "Apache License 2.0; source and model attribution are shown in Model details."
    private const val mitAttribution = "MIT licensed upstream model bundle; attribution is shown in Model details."
    private const val whisperRepository = "csukuangfj/sherpa-onnx-whisper-base"
    private const val whisperRevision = "bb53ee204431c90d314c1cc08d28d23e5b7927cc"
    private const val supertonicRepository = "csukuangfj2/sherpa-onnx-supertonic-3-tts-int8-2026-05-11"
    private const val supertonicRevision = "cca5a0e6c96e1d2c720986bf7e75fcc81dee3ae4"
    private const val piperDownloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-lessac-medium.tar.bz2"
    private const val kokoroDownloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2"
    private const val pocketTtsDownloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/sherpa-onnx-pocket-tts-int8-2026-01-26.tar.bz2"

    val entries: List<CatalogModel> = listOf(
        CatalogModel(
            manifest = ModelManifest(
                modelId = ModelId("qwen3-1.7b-q4-k-m"),
                displayName = "Qwen3 1.7B Q4_K_M",
                family = "Qwen3",
                capabilities = setOf(AiCapability.CHAT),
                engineId = EngineId("llama.cpp"),
                profileType = ModelProfileIds.LLM,
                format = ModelFormat.GGUF,
                quantization = "Q4_K_M",
                architecture = "Qwen3",
                revision = "daeb8e2d528a760970442092f6bf1e55c3b659eb",
                files = listOf(ModelFileSpec("Qwen3-1.7B-Q4_K_M.gguf", ModelFileRoles.PRIMARY_MODEL,
                    expectedBytes = 1_282_439_264, sha256 = "d2387ca2dbfee2ffabce7120d3770dadca0b293052bc2f0e138fdc940d9bc7b5")),
                source = ModelSource(
                    url = "https://huggingface.co/ggml-org/Qwen3-1.7B-GGUF/resolve/daeb8e2d528a760970442092f6bf1e55c3b659eb/Qwen3-1.7B-Q4_K_M.gguf",
                    revision = "daeb8e2d528a760970442092f6bf1e55c3b659eb",
                    licenseName = "Apache-2.0",
                    attribution = apacheAttribution,
                ),
                languages = linkedSetOf("English", "Russian", "Chinese"),
                supportedLanguageCount = 119,
                contextSize = 512,
                approximateRamBytes = 2_300_000_000,
                catalogVersion = catalogVersion,
                installedAtEpochMs = 0,
            ),
            state = ModelCatalogState.APPROVED,
            download = CatalogDownload(
                url = "https://huggingface.co/ggml-org/Qwen3-1.7B-GGUF/resolve/daeb8e2d528a760970442092f6bf1e55c3b659eb/Qwen3-1.7B-Q4_K_M.gguf",
                expectedBytes = 1_282_439_264,
                sha256 = "d2387ca2dbfee2ffabce7120d3770dadca0b293052bc2f0e138fdc940d9bc7b5",
            ),
        ),
        CatalogModel(
            manifest = ModelManifest(
                modelId = ModelId("pocket-tts-int8-en-2026-01-26"),
                displayName = "Pocket TTS INT8 (English)",
                family = "Pocket TTS",
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
        CatalogModel(
            manifest = ModelManifest(
                modelId = ModelId("whisper-base-int8"), displayName = "Whisper Base INT8", family = "Whisper",
                capabilities = setOf(AiCapability.SPEECH_TO_TEXT), engineId = EngineId("sherpa-onnx"),
                profileType = ModelProfileIds.WHISPER_STT, format = ModelFormat.ONNX, quantization = "INT8",
                revision = whisperRevision,
                files = listOf(
                    ModelFileSpec(
                        "base-encoder.int8.onnx",
                        ModelFileRoles.ENCODER,
                        expectedBytes = 29_120_534,
                        sha256 = "0b8fb1304b6109976038efff5ace81720e00386f3ff6b54ee8c75291ca0a1e11",
                    ),
                    ModelFileSpec(
                        "base-decoder.int8.onnx",
                        ModelFileRoles.DECODER,
                        expectedBytes = 130_672_026,
                        sha256 = "9759d217388a01b3a4c7c15533201067b48ae819c4daafc8624e64b9409dc02d",
                    ),
                    ModelFileSpec(
                        "base-tokens.txt",
                        ModelFileRoles.TOKENS,
                        expectedBytes = 816_730,
                        sha256 = "b34b360dbb493e781e479794586d661700670d65564001f23024971d1f2fa126",
                    ),
                ),
                source = ModelSource(
                    url = "https://huggingface.co/$whisperRepository/tree/$whisperRevision",
                    revision = whisperRevision,
                    licenseName = "Apache-2.0",
                    attribution = apacheAttribution,
                ),
                languages = linkedSetOf("English", "Russian", "Spanish"),
                supportedLanguageCount = 99,
                sampleRateHz = 16_000,
                approximateRamBytes = 850_000_000, catalogVersion = catalogVersion, installedAtEpochMs = 0,
            ),
            state = ModelCatalogState.APPROVED,
            download = CatalogDownload(
                expectedBytes = 160_609_290,
                files = huggingFaceFiles(
                    whisperRepository,
                    whisperRevision,
                    "base-encoder.int8.onnx",
                    "base-decoder.int8.onnx",
                    "base-tokens.txt",
                ),
            ),
        ),
        CatalogModel(
            manifest = ModelManifest(
                modelId = ModelId("silero-vad"), displayName = "Silero VAD", family = "Silero",
                capabilities = setOf(AiCapability.VOICE_ACTIVITY_DETECTION),
                engineId = EngineId("sherpa-onnx"), profileType = ModelProfileIds.SILERO_VAD,
                format = ModelFormat.ONNX, files = listOf(ModelFileSpec("silero_vad.onnx", ModelFileRoles.VAD_MODEL,
                    expectedBytes = 643_854, sha256 = "9e2449e1087496d8d4caba907f23e0bd3f78d91fa552479bb9c23ac09cbb1fd6")),
                source = ModelSource(
                    url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx",
                    licenseName = "MIT", attribution = mitAttribution,
                ),
                sampleRateHz = 16_000, approximateRamBytes = 20_000_000,
                catalogVersion = catalogVersion, installedAtEpochMs = 0,
            ),
            state = ModelCatalogState.APPROVED,
            download = CatalogDownload(
                url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx",
                expectedBytes = 643_854,
                sha256 = "9e2449e1087496d8d4caba907f23e0bd3f78d91fa552479bb9c23ac09cbb1fd6",
            ),
        ),
        CatalogModel(
            manifest = ModelManifest(
                modelId = ModelId("supertonic-3-int8"), displayName = "Supertonic 3 INT8", family = "Supertonic",
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
}
