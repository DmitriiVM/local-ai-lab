package com.dmitriim.localailab.ai.sherpa.catalog.tts

import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.api.model.library.CatalogDownload
import com.dmitriim.localailab.ai.api.model.library.CatalogDownloadArchive
import com.dmitriim.localailab.ai.api.model.library.CatalogModel
import com.dmitriim.localailab.ai.api.model.library.ModelCatalogDefaults
import com.dmitriim.localailab.ai.api.model.library.ModelCatalogState
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileSpec
import com.dmitriim.localailab.ai.api.model.manifest.ModelFormat
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.ai.api.model.manifest.ModelSource
import com.dmitriim.localailab.ai.sherpa.tts.profiles.KokoroTtsArtifacts
import com.dmitriim.localailab.ai.sherpa.tts.profiles.KokoroTtsProfile
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

private const val kokoroDownloadUrl =
    "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2"

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class KokoroMultiLangV1ModelDefinition(
    override val runtimeProfile: KokoroTtsProfile,
) : ModelCatalogContribution {
    override val catalogModel: CatalogModel = CatalogModel(
        manifest = ModelManifest(
            modelId = ModelId("kokoro-multi-lang-v1-0"),
            displayName = "Kokoro Multi-Lang v1.0",
            family = "Kokoro",
            description = "An English-and-Chinese text-to-speech model with 53 bundled voices.",
            capabilities = setOf(AiCapability.TEXT_TO_SPEECH),
            engineId = runtimeProfile.key.engineId,
            profileType = runtimeProfile.key.profileId,
            format = ModelFormat.ONNX,
            revision = "tts-models",
            files = listOf(
                ModelFileSpec(
                    relativePath = "model.onnx",
                    role = KokoroTtsArtifacts.MODEL,
                    expectedBytes = 325_630_829,
                    sha256 = "c436dc6a842b62aba06af67e40bafcfb9c60ac3af895358f1974ad9a7f7c026b",
                ),
                ModelFileSpec(
                    relativePath = "voices.bin",
                    role = KokoroTtsArtifacts.VOICES,
                    expectedBytes = 27_678_720,
                    sha256 = "8a77c0d397026208d22211f37670b5b3b11e03f190756b25a1d24041fced82a9",
                ),
                ModelFileSpec(
                    relativePath = "tokens.txt",
                    role = ModelFileRoles.TOKENS,
                    expectedBytes = 687,
                    sha256 = "6ebb6bb288f20f3ae8d004d3c2ca27697da27c037d75e81a60e2a6a663f95425",
                ),
                ModelFileSpec(
                    relativePath = "lexicon-us-en.txt",
                    role = KokoroTtsArtifacts.LEXICON,
                    expectedBytes = 5_956_885,
                    sha256 = "7daaab53a181be9885b853a8582bf1838186317e5dadacbcef9c426d6fa0da14",
                ),
                ModelFileSpec(
                    relativePath = "lexicon-zh.txt",
                    role = KokoroTtsArtifacts.LEXICON,
                    expectedBytes = 2_364_621,
                    sha256 = "509a1f55bf9c62e3f7e598e7544b114eadef1e00266f2badff4f281153f9f327",
                ),
                ModelFileSpec(
                    relativePath = "date-zh.fst",
                    role = KokoroTtsArtifacts.TEXT_RULES,
                    expectedBytes = 59_154,
                    sha256 = "eb8aa079ae3cb81d8f4404992f39d61a0cb990947512b5b8d1e54d1f6980e718",
                ),
                ModelFileSpec(
                    relativePath = "number-zh.fst",
                    role = KokoroTtsArtifacts.TEXT_RULES,
                    expectedBytes = 64_482,
                    sha256 = "743f402181fcfebf76cc2f0546b71fa26476e626fbe4e460fb7b4c3a7a8bd5bd",
                ),
                ModelFileSpec(
                    relativePath = "phone-zh.fst",
                    role = KokoroTtsArtifacts.TEXT_RULES,
                    expectedBytes = 88_630,
                    sha256 = "1ac2b6fa56b1442320c4de7db08353bab8963a2b57f365eebcdd3a2d3562f8d7",
                ),
                ModelFileSpec("espeak-ng-data", ModelFileRoles.FRONTEND_DATA, directory = true),
                ModelFileSpec("dict", KokoroTtsArtifacts.DICTIONARY, directory = true),
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
            catalogVersion = ModelCatalogDefaults.VERSION,
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
    )
}
