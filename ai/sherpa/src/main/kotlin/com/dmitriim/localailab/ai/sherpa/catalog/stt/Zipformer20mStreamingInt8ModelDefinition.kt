package com.dmitriim.localailab.ai.sherpa.catalog.stt

import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.sherpa.stt.profiles.ZipformerSttProfile
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileSpec
import com.dmitriim.localailab.ai.api.model.manifest.SttRecognitionMode
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class Zipformer20mStreamingInt8ModelDefinition(
    override val runtimeProfile: ZipformerSttProfile,
) : ModelCatalogContribution {
    override val catalogModel = archiveSttCatalogModel(
        modelId = "zipformer-en-20m-streaming-int8",
        displayName = "Zipformer 20M Streaming INT8",
        family = "Zipformer",
        description = "An English streaming speech-to-text model quantized to INT8.",
        profileKey = runtimeProfile.key,
        archiveName = "sherpa-onnx-streaming-zipformer-en-20M-2023-02-17",
        archiveBytes = 127_887_156,
        archiveSha256 = "9c559283e8498d3fe95913c79ca1cb454bb26281ac2b102b41306c7d752765d9",
        files = listOf(
            ModelFileSpec("encoder-epoch-99-avg-1.int8.onnx", ModelFileRoles.ENCODER, expectedBytes = 42_845_182),
            ModelFileSpec("decoder-epoch-99-avg-1.int8.onnx", ModelFileRoles.DECODER, expectedBytes = 539_499),
            ModelFileSpec("joiner-epoch-99-avg-1.int8.onnx", ModelFileRoles.JOINER, expectedBytes = 259_572),
            ModelFileSpec("tokens.txt", ModelFileRoles.TOKENS, expectedBytes = 5_048),
        ),
        languages = linkedSetOf("English"),
        licenseName = "Apache-2.0",
        attribution = "Icefall streaming Zipformer model packaged for sherpa-onnx.",
        recognitionMode = SttRecognitionMode.STREAMING,
        approximateRamBytes = 350_000_000,
    )
}
