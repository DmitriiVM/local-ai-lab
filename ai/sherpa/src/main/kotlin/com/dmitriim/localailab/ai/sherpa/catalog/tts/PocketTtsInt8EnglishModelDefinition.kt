package com.dmitriim.localailab.ai.sherpa.catalog.tts

import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.api.model.library.CatalogDownload
import com.dmitriim.localailab.ai.api.model.library.CatalogDownloadArchive
import com.dmitriim.localailab.ai.api.model.library.CatalogModel
import com.dmitriim.localailab.ai.api.model.library.ModelCatalogDefaults
import com.dmitriim.localailab.ai.api.model.library.ModelCatalogState
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileSpec
import com.dmitriim.localailab.ai.api.model.manifest.ModelFormat
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.ai.api.model.manifest.ModelSource
import com.dmitriim.localailab.ai.sherpa.tts.profiles.PocketTtsArtifacts
import com.dmitriim.localailab.ai.sherpa.tts.profiles.PocketTtsProfile
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

private const val pocketTtsDownloadUrl =
    "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/" +
        "sherpa-onnx-pocket-tts-int8-2026-01-26.tar.bz2"

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class PocketTtsInt8EnglishModelDefinition(
    override val runtimeProfile: PocketTtsProfile,
) : ModelCatalogContribution {
    override val catalogModel: CatalogModel = CatalogModel(
        manifest = ModelManifest(
            modelId = ModelId("pocket-tts-int8-en-2026-01-26"),
            displayName = "Pocket TTS INT8 (English)",
            family = "Pocket TTS",
            description = "An English reference-voice text-to-speech model with bundled reference audio.",
            capabilities = setOf(AiCapability.TEXT_TO_SPEECH),
            engineId = runtimeProfile.key.engineId,
            profileType = runtimeProfile.key.profileId,
            format = ModelFormat.ONNX,
            quantization = "INT8",
            architecture = "Pocket TTS",
            revision = "2026-01-26",
            files = listOf(
                ModelFileSpec(
                    relativePath = "lm_flow.int8.onnx",
                    role = PocketTtsArtifacts.LM_FLOW,
                    expectedBytes = 9_962_530,
                    sha256 = "8d627d235c44a597da908e1085ebe241cbbe358964c502c5a5063d18851a5529",
                ),
                ModelFileSpec(
                    relativePath = "lm_main.int8.onnx",
                    role = PocketTtsArtifacts.LM_MAIN,
                    expectedBytes = 76_341_079,
                    sha256 = "bfc0c7e7e3d72864fa3bb2ee499f62f21ddc1474b885f5f3ca570f8be73e787e",
                ),
                ModelFileSpec(
                    relativePath = "encoder.onnx",
                    role = PocketTtsArtifacts.ENCODER,
                    expectedBytes = 72_713_165,
                    sha256 = "e8f2f6d301ffb96e398b138a7dc6d3038622d236044636b73d920bab85890260",
                ),
                ModelFileSpec(
                    relativePath = "decoder.int8.onnx",
                    role = PocketTtsArtifacts.DECODER,
                    expectedBytes = 22_693_618,
                    sha256 = "12b0857402d31aead94df19d6783b4350d1f740e811f3a3202c70ad89ae11eea",
                ),
                ModelFileSpec(
                    relativePath = "text_conditioner.onnx",
                    role = PocketTtsArtifacts.TEXT_CONDITIONER,
                    expectedBytes = 16_388_343,
                    sha256 = "0b84e837d7bfaf2c896627b03e3f080320309f37f4fc7df7698c644f7ba5e6b1",
                ),
                ModelFileSpec(
                    relativePath = "vocab.json",
                    role = PocketTtsArtifacts.VOCABULARY,
                    expectedBytes = 69_478,
                    sha256 = "6fb646346cf931016f70c4921aab0900ce7a304b893cb02135c74e294abfea01",
                ),
                ModelFileSpec(
                    relativePath = "token_scores.json",
                    role = PocketTtsArtifacts.TOKEN_SCORES,
                    expectedBytes = 123_616,
                    sha256 = "5be2f278caf9b9800741f0fd82bff677f4943ec764c356f907213434b622d958",
                ),
                ModelFileSpec(
                    relativePath = "test_wavs/bria.wav",
                    role = PocketTtsArtifacts.REFERENCE_AUDIO,
                    expectedBytes = 2_152_986,
                    sha256 = "85f46d6f0642f657a6bd689ddaa52d5a5f53e4314715e1032704c80917392181",
                ),
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
            catalogVersion = ModelCatalogDefaults.VERSION,
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
    )
}
