package com.dmitriim.localailab.ai.sherpa.catalog

import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.library.ModelCatalogContribution
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class WhisperTinyInt8ModelDefinition : ModelCatalogContribution {
    override val catalogModel = whisperCatalogModel(
        modelId = "whisper-tiny-int8",
        displayName = "Whisper Tiny INT8",
        description = "A multilingual Whisper Tiny speech-to-text model quantized to INT8 for offline transcription.",
        repository = "csukuangfj/sherpa-onnx-whisper-tiny",
        revision = "65176e2deb88badc814a94058666cadccc29b61c",
        filePrefix = "tiny",
        encoderBytes = 12_937_772,
        encoderSha256 = "d24fb083ae3b1041fc24e97971d60e280c9342201fbb67b0ab428a8b4a51a434",
        decoderBytes = 89_855_401,
        decoderSha256 = "d2fece8dd42771f1df975c6c0445770d0c292bf7547c2cae04a6c0cc57540925",
        approximateRamBytes = 500_000_000,
    )
}
