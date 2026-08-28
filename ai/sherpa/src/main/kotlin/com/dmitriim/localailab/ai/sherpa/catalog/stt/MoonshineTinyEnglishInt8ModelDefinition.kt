package com.dmitriim.localailab.ai.sherpa.catalog.stt

import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.sherpa.stt.profiles.MoonshineV1SttProfile
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.library.CatalogDownload
import com.dmitriim.localailab.core.model.library.CatalogDownloadArchive
import com.dmitriim.localailab.core.model.library.CatalogModel
import com.dmitriim.localailab.core.model.library.ModelCatalogDefaults
import com.dmitriim.localailab.core.model.library.ModelCatalogState
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelFileSpec
import com.dmitriim.localailab.core.model.manifest.ModelFormat
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.manifest.ModelSource
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class MoonshineTinyEnglishInt8ModelDefinition(
    override val runtimeProfile: MoonshineV1SttProfile,
) : ModelCatalogContribution {
    override val catalogModel = CatalogModel(
        manifest = ModelManifest(
            modelId = ModelId("moonshine-tiny-en-int8"),
            displayName = "Moonshine Tiny English INT8",
            family = "Moonshine",
            description = "A compact English offline speech-to-text model quantized to INT8.",
            capabilities = setOf(AiCapability.SPEECH_TO_TEXT),
            engineId = runtimeProfile.key.engineId,
            profileType = runtimeProfile.key.profileId,
            format = ModelFormat.ONNX,
            quantization = "INT8",
            architecture = "Moonshine",
            revision = "int8",
            files = listOf(
                ModelFileSpec("preprocess.onnx", ModelFileRoles.PREPROCESSOR, 6_800_738),
                ModelFileSpec("encode.int8.onnx", ModelFileRoles.ENCODER, 18_249_187),
                ModelFileSpec("uncached_decode.int8.onnx", ModelFileRoles.UNCACHED_DECODER, 53_216_096),
                ModelFileSpec("cached_decode.int8.onnx", ModelFileRoles.CACHED_DECODER, 45_264_830),
                ModelFileSpec("tokens.txt", ModelFileRoles.TOKENS, 436_688),
            ),
            source = ModelSource(
                url = DOWNLOAD_URL,
                revision = ARCHIVE_NAME,
                licenseName = "MIT",
                attribution = "Moonshine Tiny by Useful Sensors, converted and packaged for sherpa-onnx.",
            ),
            languages = linkedSetOf("English"),
            supportedLanguageCount = 1,
            sampleRateHz = 16_000,
            approximateRamBytes = 350_000_000,
            catalogVersion = ModelCatalogDefaults.VERSION,
            installedAtEpochMs = 0,
        ),
        state = ModelCatalogState.APPROVED,
        download = CatalogDownload(
            expectedBytes = 107_600_538,
            archive = CatalogDownloadArchive(
                url = DOWNLOAD_URL,
                expectedBytes = 107_600_538,
                sha256 = "d5fe6ec4334fef36255b2a4010412cad4c007e33103fec62fb5d17cad88086f2",
                rootDirectory = ARCHIVE_NAME,
            ),
        ),
    )

    private companion object {
        const val ARCHIVE_NAME = "sherpa-onnx-moonshine-tiny-en-int8"
        const val DOWNLOAD_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/$ARCHIVE_NAME.tar.bz2"
    }
}
