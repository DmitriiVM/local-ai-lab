package com.dmitriim.localailab.source.models.catalog.stt

import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.library.CatalogDownload
import com.dmitriim.localailab.core.model.library.CatalogModel
import com.dmitriim.localailab.core.model.library.ModelCatalogState
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelFileSpec
import com.dmitriim.localailab.core.model.manifest.ModelFormat
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.manifest.ModelSource
import com.dmitriim.localailab.source.models.catalog.CatalogDefaults
import com.dmitriim.localailab.source.models.catalog.download.huggingFaceFiles

internal object WhisperModelCatalog {
    private const val whisperTinyRepository = "csukuangfj/sherpa-onnx-whisper-tiny"
    private const val whisperTinyRevision = "65176e2deb88badc814a94058666cadccc29b61c"
    private const val whisperBaseRepository = "csukuangfj/sherpa-onnx-whisper-base"
    private const val whisperBaseRevision = "bb53ee204431c90d314c1cc08d28d23e5b7927cc"
    private const val whisperSmallRepository = "csukuangfj/sherpa-onnx-whisper-small"
    private const val whisperSmallRevision = "8f3c18b358db4d1f2fc1eae49d75cd20989e4309"

    val entries: List<CatalogModel> = listOf(
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
    )

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
                    attribution = CatalogDefaults.APACHE_ATTRIBUTION,
                ),
                languages = linkedSetOf("English", "Russian", "Spanish"),
                supportedLanguageCount = 99,
                sampleRateHz = 16_000,
                approximateRamBytes = approximateRamBytes,
                catalogVersion = CatalogDefaults.VERSION,
                installedAtEpochMs = 0,
            ),
            state = ModelCatalogState.APPROVED,
            download = CatalogDownload(
                expectedBytes = encoderBytes + decoderBytes + tokenBytes,
                files = huggingFaceFiles(repository, revision, encoder, decoder, tokens),
            ),
        )
    }
}
