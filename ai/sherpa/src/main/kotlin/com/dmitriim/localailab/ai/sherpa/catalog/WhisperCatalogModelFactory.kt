package com.dmitriim.localailab.ai.sherpa.catalog

import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.library.CatalogDownload
import com.dmitriim.localailab.core.model.library.CatalogModel
import com.dmitriim.localailab.core.model.library.ModelCatalogDefaults
import com.dmitriim.localailab.core.model.library.ModelCatalogState
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelFileSpec
import com.dmitriim.localailab.core.model.manifest.ModelFormat
import com.dmitriim.localailab.core.model.manifest.ModelId
import com.dmitriim.localailab.core.model.manifest.ModelManifest
import com.dmitriim.localailab.core.model.manifest.ModelProfileKey
import com.dmitriim.localailab.core.model.manifest.ModelSource

internal fun whisperCatalogModel(
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
    profileKey: ModelProfileKey,
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
            engineId = profileKey.engineId,
            profileType = profileKey.profileId,
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
                attribution = ModelCatalogDefaults.APACHE_ATTRIBUTION,
            ),
            languages = linkedSetOf("English", "Russian", "Spanish"),
            supportedLanguageCount = 99,
            sampleRateHz = 16_000,
            approximateRamBytes = approximateRamBytes,
            catalogVersion = ModelCatalogDefaults.VERSION,
            installedAtEpochMs = 0,
        ),
        state = ModelCatalogState.APPROVED,
        download = CatalogDownload(
            expectedBytes = encoderBytes + decoderBytes + tokenBytes,
            files = huggingFaceFiles(repository, revision, encoder, decoder, tokens),
        ),
    )
}
