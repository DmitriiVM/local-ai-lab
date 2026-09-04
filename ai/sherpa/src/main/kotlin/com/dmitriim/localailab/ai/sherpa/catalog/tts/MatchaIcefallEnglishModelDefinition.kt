package com.dmitriim.localailab.ai.sherpa.catalog.tts

import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.api.model.library.CatalogDownload
import com.dmitriim.localailab.ai.api.model.library.CatalogDownloadArchive
import com.dmitriim.localailab.ai.api.model.library.CatalogDownloadFile
import com.dmitriim.localailab.ai.api.model.library.CatalogModel
import com.dmitriim.localailab.ai.api.model.library.ModelCatalogDefaults
import com.dmitriim.localailab.ai.api.model.library.ModelCatalogState
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileSpec
import com.dmitriim.localailab.ai.api.model.manifest.ModelFormat
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.ai.api.model.manifest.ModelSource
import com.dmitriim.localailab.ai.sherpa.tts.profiles.MatchaTtsArtifacts
import com.dmitriim.localailab.ai.sherpa.tts.profiles.MatchaTtsProfile
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

private const val matchaArchiveUrl =
    "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/" +
        "matcha-icefall-en_US-ljspeech.tar.bz2"
private const val vocosDownloadUrl =
    "https://github.com/k2-fsa/sherpa-onnx/releases/download/vocoder-models/vocos-22khz-univ.onnx"

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class MatchaIcefallEnglishModelDefinition(
    override val runtimeProfile: MatchaTtsProfile,
) : ModelCatalogContribution {
    override val catalogModel: CatalogModel = CatalogModel(
        manifest = ModelManifest(
            modelId = ModelId("matcha-icefall-en-us-ljspeech"),
            displayName = "Matcha Icefall (English)",
            family = "Matcha-TTS",
            description = "A single-speaker English flow-matching text-to-speech model using the LJSpeech voice.",
            capabilities = setOf(AiCapability.TEXT_TO_SPEECH),
            engineId = runtimeProfile.key.engineId,
            profileType = runtimeProfile.key.profileId,
            format = ModelFormat.ONNX,
            architecture = "Matcha-TTS + Vocos",
            revision = "tts-models",
            files = listOf(
                ModelFileSpec(
                    relativePath = "model-steps-3.onnx",
                    role = MatchaTtsArtifacts.ACOUSTIC_MODEL,
                    expectedBytes = 74_157_257,
                    sha256 = "4d7771c0ec063ca74f2fa92ba0ecfe14f73fa3f61c236d9469b92e102d8e9574",
                ),
                ModelFileSpec(
                    relativePath = "vocos-22khz-univ.onnx",
                    role = ModelFileRoles.VOCODER,
                    expectedBytes = 53_884_024,
                    sha256 = "0574a135aa1db2de6e181050db2ec528496cacd4a4701fc5d7faf9f9804c0081",
                ),
                ModelFileSpec(
                    relativePath = "tokens.txt",
                    role = ModelFileRoles.TOKENS,
                    expectedBytes = 954,
                    sha256 = "bcb4a50830e9402112fe8cb57d53ae8523908868a5015f2040dde5f5fd231697",
                ),
                ModelFileSpec(
                    relativePath = "espeak-ng-data",
                    role = ModelFileRoles.FRONTEND_DATA,
                    directory = true,
                ),
            ),
            source = ModelSource(
                url = matchaArchiveUrl,
                revision = "tts-models",
                licenseName = "Upstream Matcha-TTS and Vocos model terms",
                attribution = "Matcha-TTS acoustic model and Vocos vocoder package distributed by sherpa-onnx.",
            ),
            languages = linkedSetOf("English"),
            supportedLanguageCount = 1,
            sampleRateHz = 22_050,
            speakerCount = 1,
            voices = listOf(
                ttsVoice(
                    id = "ljspeech",
                    displayName = "LJSpeech",
                    speakerId = 0,
                    description = "English (United States) · Single speaker",
                    languages = arrayOf("en"),
                ),
            ),
            approximateRamBytes = 300_000_000,
            catalogVersion = ModelCatalogDefaults.VERSION,
            installedAtEpochMs = 0,
        ),
        state = ModelCatalogState.APPROVED,
        download = CatalogDownload(
            expectedBytes = 130_625_145,
            files = listOf(
                CatalogDownloadFile(
                    relativePath = "vocos-22khz-univ.onnx",
                    url = vocosDownloadUrl,
                ),
            ),
            archive = CatalogDownloadArchive(
                url = matchaArchiveUrl,
                expectedBytes = 76_741_121,
                sha256 = "ea75702da7456a8b1874728278a835220dc8a26f4e8bd93c83bf53dc27679845",
                rootDirectory = "matcha-icefall-en_US-ljspeech",
            ),
        ),
    )
}
