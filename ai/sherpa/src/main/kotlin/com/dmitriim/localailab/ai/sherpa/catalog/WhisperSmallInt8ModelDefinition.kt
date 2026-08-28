package com.dmitriim.localailab.ai.sherpa.catalog

import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.sherpa.stt.WhisperSttProfile
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class WhisperSmallInt8ModelDefinition(
    override val runtimeProfile: WhisperSttProfile,
) : ModelCatalogContribution {
    override val catalogModel = whisperCatalogModel(
        modelId = "whisper-small-int8",
        displayName = "Whisper Small INT8",
        description = "A multilingual Whisper Small speech-to-text model quantized to INT8 for offline transcription.",
        repository = "csukuangfj/sherpa-onnx-whisper-small",
        revision = "8f3c18b358db4d1f2fc1eae49d75cd20989e4309",
        filePrefix = "small",
        encoderBytes = 112_442_483,
        encoderSha256 = "4cbe7b22fa9026b843b60a68640c747de05bafb1a11b57edc0e66c232d9f33a9",
        decoderBytes = 262_226_114,
        decoderSha256 = "acad50b5c782696e91b55914cc5ab4f756f1532f76e22aa6fc615f39fb69a8ee",
        approximateRamBytes = 2_600_000_000,
    )
}
