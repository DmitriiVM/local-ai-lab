package com.dmitriim.localailab.ai.sherpa.catalog.tts

import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.sherpa.catalog.huggingFaceFiles
import com.dmitriim.localailab.ai.sherpa.tts.profiles.SupertonicTtsArtifacts
import com.dmitriim.localailab.ai.sherpa.tts.profiles.SupertonicTtsProfile
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.model.library.CatalogDownload
import com.dmitriim.localailab.ai.api.model.library.CatalogModel
import com.dmitriim.localailab.ai.api.model.library.ModelCatalogDefaults
import com.dmitriim.localailab.ai.api.model.library.ModelCatalogState
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileSpec
import com.dmitriim.localailab.ai.api.model.manifest.ModelFormat
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.ai.api.model.manifest.ModelSource
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

private const val supertonicRepository = "csukuangfj2/sherpa-onnx-supertonic-3-tts-int8-2026-05-11"
private const val supertonicRevision = "cca5a0e6c96e1d2c720986bf7e75fcc81dee3ae4"

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class Supertonic3Int8ModelDefinition(
    override val runtimeProfile: SupertonicTtsProfile,
) : ModelCatalogContribution {
    override val catalogModel: CatalogModel = CatalogModel(
        manifest = ModelManifest(
            modelId = ModelId("supertonic-3-int8"), displayName = "Supertonic 3 INT8", family = "Supertonic",
            description = "A multilingual text-to-speech model with 10 bundled voices.",
            capabilities = setOf(AiCapability.TEXT_TO_SPEECH),
            engineId = runtimeProfile.key.engineId,
            profileType = runtimeProfile.key.profileId,
            format = ModelFormat.ONNX, quantization = "INT8",
            revision = supertonicRevision,
            files = listOf(
                ModelFileSpec(
                    "duration_predictor.int8.onnx",
                    SupertonicTtsArtifacts.DURATION_PREDICTOR,
                    expectedBytes = 3_700_147,
                    sha256 = "c3eb91414d5ff8a7a239b7fe9e34e7e2bf8a8140d8375ffb14718b1c639325db",
                ),
                ModelFileSpec(
                    "text_encoder.int8.onnx",
                    SupertonicTtsArtifacts.TEXT_ENCODER,
                    expectedBytes = 36_416_150,
                    sha256 = "c7befd5ea8c3119769e8a6c1486c4edc6a3bc8365c67621c881bbb774b9902ff",
                ),
                ModelFileSpec(
                    "vector_estimator.int8.onnx",
                    SupertonicTtsArtifacts.VECTOR_ESTIMATOR,
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
                    SupertonicTtsArtifacts.UNICODE_INDEXER,
                    expectedBytes = 262_144,
                    sha256 = "8402ca48e5189a8950138580b0fff64db6f072f24ac07cd54ba8b2fbb9883b30",
                ),
                ModelFileSpec(
                    "voice.bin",
                    SupertonicTtsArtifacts.VOICE_STYLE,
                    expectedBytes = 517_168,
                    sha256 = "67d5209b0ee8ce6c74105ffbe12fe6a7628aea3b4ba2fcb308a4a67938a93ce8",
                ),
            ),
            source = ModelSource(
                url = "https://huggingface.co/$supertonicRepository/tree/$supertonicRevision",
                revision = supertonicRevision,
                licenseName = "MIT",
                attribution = ModelCatalogDefaults.MIT_ATTRIBUTION,
            ),
            languages = linkedSetOf("English", "Russian", "German"), supportedLanguageCount = 31, sampleRateHz = 44_100,
            speakerCount = 10,
            voices = supertonicV3Voices(),
            approximateRamBytes = 900_000_000, catalogVersion = ModelCatalogDefaults.VERSION, installedAtEpochMs = 0,
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
    )
}
