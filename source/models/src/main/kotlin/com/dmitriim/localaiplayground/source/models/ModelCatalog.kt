package com.dmitriim.localaiplayground.source.models

import com.dmitriim.localaiplayground.core.model.AiCapability
import com.dmitriim.localaiplayground.core.model.CatalogDownload
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
                files = listOf(
                    ModelFileSpec("base-encoder.int8.onnx", ModelFileRole.ENCODER),
                    ModelFileSpec("base-decoder.int8.onnx", ModelFileRole.DECODER),
                    ModelFileSpec("base-tokens.txt", ModelFileRole.TOKENS),
                ),
                source = ModelSource(
                    url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-base.tar.bz2",
                    licenseName = "Apache-2.0", attribution = apacheAttribution,
                ),
                languages = setOf("English", "Russian"), sampleRateHz = 16_000,
                approximateRamBytes = 850_000_000, catalogVersion = catalogVersion, installedAtEpochMs = 0,
            ),
            state = ModelCatalogState.APPROVED,
            download = CatalogDownload(
                url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-base.tar.bz2",
                expectedBytes = 207_557_382,
                sha256 = "911b2083efd7c0dca2ac3b358b75222660dc09fb716d64fbfc417ba6c99ff3de",
                archive = true,
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
                files = listOf(
                    ModelFileSpec("duration_predictor.int8.onnx", ModelFileRole.DURATION_PREDICTOR),
                    ModelFileSpec("text_encoder.int8.onnx", ModelFileRole.TEXT_ENCODER),
                    ModelFileSpec("vector_estimator.int8.onnx", ModelFileRole.VECTOR_ESTIMATOR),
                    ModelFileSpec("vocoder.int8.onnx", ModelFileRole.VOCODER),
                    ModelFileSpec("tts.json", ModelFileRole.CONFIG),
                    ModelFileSpec("unicode_indexer.bin", ModelFileRole.UNICODE_INDEXER),
                    ModelFileSpec("voice.bin", ModelFileRole.VOICE_STYLE),
                ),
                source = ModelSource(
                    url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/sherpa-onnx-supertonic-3-tts-int8-2026-05-11.tar.bz2",
                    licenseName = "MIT", attribution = mitAttribution,
                ),
                languages = setOf("English", "Russian"), sampleRateHz = 44_100,
                approximateRamBytes = 900_000_000, catalogVersion = catalogVersion, installedAtEpochMs = 0,
            ),
            state = ModelCatalogState.APPROVED,
            download = CatalogDownload(
                url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/sherpa-onnx-supertonic-3-tts-int8-2026-05-11.tar.bz2",
                expectedBytes = 128_774_318,
                sha256 = "82fa96f91c4ef8abaae3a14a3f4153facf88bed821d1f7331cec2700f432c427",
                archive = true,
            ),
        ),
    )
}
