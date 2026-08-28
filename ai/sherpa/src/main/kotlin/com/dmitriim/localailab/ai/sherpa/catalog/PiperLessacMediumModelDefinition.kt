package com.dmitriim.localailab.ai.sherpa.catalog

import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.library.CatalogDownload
import com.dmitriim.localailab.core.model.library.CatalogDownloadArchive
import com.dmitriim.localailab.core.model.library.CatalogModel
import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.sherpa.tts.PiperTtsProfile
import com.dmitriim.localailab.core.model.library.ModelCatalogDefaults
import com.dmitriim.localailab.core.model.library.ModelCatalogState
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelFileSpec
import com.dmitriim.localailab.core.model.manifest.ModelFormat
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.manifest.ModelSource
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

private const val piperDownloadUrl =
    "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/" +
        "vits-piper-en_US-lessac-medium.tar.bz2"

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class PiperLessacMediumModelDefinition(
    override val runtimeProfile: PiperTtsProfile,
) : ModelCatalogContribution {
    override val catalogModel: CatalogModel = CatalogModel(
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
            catalogVersion = ModelCatalogDefaults.VERSION,
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
    )
}
