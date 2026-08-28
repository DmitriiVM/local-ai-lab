package com.dmitriim.localailab.ai.sherpa.catalog

import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.sherpa.tts.KittenTtsArtifacts
import com.dmitriim.localailab.ai.sherpa.tts.KittenTtsProfile
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

private const val kittenArchiveName = "kitten-nano-en-v0_1-fp16"
private const val kittenDownloadUrl =
    "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/$kittenArchiveName.tar.bz2"

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class KittenNanoV01Fp16ModelDefinition(
    override val runtimeProfile: KittenTtsProfile,
) : ModelCatalogContribution {
    override val catalogModel = CatalogModel(
        manifest = ModelManifest(
            modelId = ModelId("kitten-nano-en-v0-1-fp16"),
            displayName = "KittenTTS Nano v0.1 (English)",
            family = "KittenTTS",
            description = "A compact English text-to-speech model with eight bundled voices.",
            capabilities = setOf(AiCapability.TEXT_TO_SPEECH),
            engineId = runtimeProfile.key.engineId,
            profileType = runtimeProfile.key.profileId,
            format = ModelFormat.ONNX,
            quantization = "FP16",
            revision = "v0.1",
            files = listOf(
                ModelFileSpec("model.fp16.onnx", KittenTtsArtifacts.MODEL),
                ModelFileSpec("voices.bin", KittenTtsArtifacts.VOICES),
                ModelFileSpec("tokens.txt", ModelFileRoles.TOKENS),
                ModelFileSpec("espeak-ng-data", ModelFileRoles.FRONTEND_DATA, directory = true),
            ),
            source = ModelSource(
                url = kittenDownloadUrl,
                revision = "v0.1",
                licenseName = "Apache-2.0",
                attribution = ModelCatalogDefaults.APACHE_ATTRIBUTION,
            ),
            languages = linkedSetOf("English"),
            supportedLanguageCount = 1,
            sampleRateHz = 24_000,
            speakerCount = 8,
            voices = kittenNanoV01Voices(),
            approximateRamBytes = 120_000_000,
            catalogVersion = ModelCatalogDefaults.VERSION,
            installedAtEpochMs = 0,
        ),
        state = ModelCatalogState.APPROVED,
        download = CatalogDownload(
            expectedBytes = 26_855_312,
            archive = CatalogDownloadArchive(
                url = kittenDownloadUrl,
                expectedBytes = 26_855_312,
                sha256 = "f35dac93754fe2ac97c66e1f468311d0d2130f7f0f5a89bfa1197e09a0cbdec5",
                rootDirectory = kittenArchiveName,
            ),
        ),
    )
}
