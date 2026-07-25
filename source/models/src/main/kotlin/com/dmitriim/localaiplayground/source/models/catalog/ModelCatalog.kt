package com.dmitriim.localaiplayground.source.models.catalog

import com.dmitriim.localaiplayground.core.model.AiCapability
import com.dmitriim.localaiplayground.core.model.CatalogDownload
import com.dmitriim.localaiplayground.core.model.CatalogDownloadFile
import com.dmitriim.localaiplayground.core.model.CatalogModel
import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.core.model.ModelCatalogState
import com.dmitriim.localaiplayground.core.model.ModelFileRole
import com.dmitriim.localaiplayground.core.model.ModelFileSpec
import com.dmitriim.localaiplayground.core.model.ModelFormat
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelManifest
import com.dmitriim.localaiplayground.core.model.ModelSource
import com.dmitriim.localaiplayground.core.model.RuntimeProfileType

/** Immutable, app-bundled catalog. Remote hosts provide bytes only, never catalog updates. */
internal object ModelCatalog {
    private const val catalogVersion = 1
    private const val apacheAttribution = "Apache License 2.0; source and model attribution are shown in Model details."
    private const val mitAttribution = "MIT licensed upstream model bundle; attribution is shown in Model details."
    private const val whisperRepository = "csukuangfj/sherpa-onnx-whisper-base"
    private const val whisperRevision = "bb53ee204431c90d314c1cc08d28d23e5b7927cc"
    private const val supertonicRepository = "csukuangfj2/sherpa-onnx-supertonic-3-tts-int8-2026-05-11"
    private const val supertonicRevision = "cca5a0e6c96e1d2c720986bf7e75fcc81dee3ae4"

    val entries: List<CatalogModel> = listOf(
        CatalogModel(
            manifest = ModelManifest(
                modelId = ModelId("qwen3-1.7b-q4-k-m"),
                displayName = "Qwen3 1.7B Q4_K_M",
                family = "Qwen3",
                capabilities = setOf(AiCapability.CHAT),
                engineId = EngineId("llama.cpp"),
                profileType = RuntimeProfileType.LLM,
                format = ModelFormat.GGUF,
                quantization = "Q4_K_M",
                architecture = "Qwen3",
                revision = "daeb8e2d528a760970442092f6bf1e55c3b659eb",
                files = listOf(ModelFileSpec("Qwen3-1.7B-Q4_K_M.gguf", ModelFileRole.PRIMARY_MODEL,
                    expectedBytes = 1_282_439_264, sha256 = "d2387ca2dbfee2ffabce7120d3770dadca0b293052bc2f0e138fdc940d9bc7b5")),
                source = ModelSource(
                    url = "https://huggingface.co/ggml-org/Qwen3-1.7B-GGUF/resolve/daeb8e2d528a760970442092f6bf1e55c3b659eb/Qwen3-1.7B-Q4_K_M.gguf",
                    revision = "daeb8e2d528a760970442092f6bf1e55c3b659eb",
                    licenseName = "Apache-2.0",
                    attribution = apacheAttribution,
                ),
                languages = setOf("English", "Russian"),
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
                modelId = ModelId("whisper-base-int8"), displayName = "Whisper Base INT8", family = "Whisper",
                capabilities = setOf(AiCapability.SPEECH_TO_TEXT), engineId = EngineId("sherpa-onnx"),
                profileType = RuntimeProfileType.WHISPER_STT, format = ModelFormat.ONNX, quantization = "INT8",
                revision = whisperRevision,
                files = listOf(
                    ModelFileSpec(
                        "base-encoder.int8.onnx",
                        ModelFileRole.ENCODER,
                        expectedBytes = 29_120_534,
                        sha256 = "0b8fb1304b6109976038efff5ace81720e00386f3ff6b54ee8c75291ca0a1e11",
                    ),
                    ModelFileSpec(
                        "base-decoder.int8.onnx",
                        ModelFileRole.DECODER,
                        expectedBytes = 130_672_026,
                        sha256 = "9759d217388a01b3a4c7c15533201067b48ae819c4daafc8624e64b9409dc02d",
                    ),
                    ModelFileSpec(
                        "base-tokens.txt",
                        ModelFileRole.TOKENS,
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
                languages = setOf("English", "Russian"), sampleRateHz = 16_000,
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
                capabilities = setOf(AiCapability.SPEECH_TO_TEXT, AiCapability.VOICE_ASSISTANT),
                engineId = EngineId("sherpa-onnx"), profileType = RuntimeProfileType.SILERO_VAD,
                format = ModelFormat.ONNX, files = listOf(ModelFileSpec("silero_vad.onnx", ModelFileRole.VAD_MODEL,
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
                capabilities = setOf(AiCapability.TEXT_TO_SPEECH, AiCapability.VOICE_ASSISTANT),
                engineId = EngineId("sherpa-onnx"), profileType = RuntimeProfileType.SUPERTONIC_TTS,
                format = ModelFormat.ONNX, quantization = "INT8",
                revision = supertonicRevision,
                files = listOf(
                    ModelFileSpec(
                        "duration_predictor.int8.onnx",
                        ModelFileRole.DURATION_PREDICTOR,
                        expectedBytes = 3_700_147,
                        sha256 = "c3eb91414d5ff8a7a239b7fe9e34e7e2bf8a8140d8375ffb14718b1c639325db",
                    ),
                    ModelFileSpec(
                        "text_encoder.int8.onnx",
                        ModelFileRole.TEXT_ENCODER,
                        expectedBytes = 36_416_150,
                        sha256 = "c7befd5ea8c3119769e8a6c1486c4edc6a3bc8365c67621c881bbb774b9902ff",
                    ),
                    ModelFileSpec(
                        "vector_estimator.int8.onnx",
                        ModelFileRole.VECTOR_ESTIMATOR,
                        expectedBytes = 78_400_833,
                        sha256 = "20cd86fa5c6effedfda0e7cffe5b0569ca401c440a0c3a1d72bf39286c0db3fd",
                    ),
                    ModelFileSpec(
                        "vocoder.int8.onnx",
                        ModelFileRole.VOCODER,
                        expectedBytes = 25_991_073,
                        sha256 = "e923d60f53f95eb1ce235f1dc33ec56d9c057823c96fa6f8acf98f32b0da6152",
                    ),
                    ModelFileSpec(
                        "tts.json",
                        ModelFileRole.CONFIG,
                        expectedBytes = 8_253,
                        sha256 = "42078d3aef1cd43ab43021f3c54f47d2d75ceb4e75f627f118890128b06a0d09",
                    ),
                    ModelFileSpec(
                        "unicode_indexer.bin",
                        ModelFileRole.UNICODE_INDEXER,
                        expectedBytes = 262_144,
                        sha256 = "8402ca48e5189a8950138580b0fff64db6f072f24ac07cd54ba8b2fbb9883b30",
                    ),
                    ModelFileSpec(
                        "voice.bin",
                        ModelFileRole.VOICE_STYLE,
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
                languages = setOf("English", "Russian"), sampleRateHz = 44_100,
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
