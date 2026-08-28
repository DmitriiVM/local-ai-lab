package com.dmitriim.localailab.ai.sherpa.catalog

import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.sherpa.stt.WhisperSttProfile
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class WhisperBaseInt8ModelDefinition(
    override val runtimeProfile: WhisperSttProfile,
) : ModelCatalogContribution {
    override val catalogModel = whisperCatalogModel(
        profileKey = runtimeProfile.key,
        modelId = "whisper-base-int8",
        displayName = "Whisper Base INT8",
        description = "A multilingual Whisper Base speech-to-text model quantized to INT8 for offline transcription.",
        repository = "csukuangfj/sherpa-onnx-whisper-base",
        revision = "bb53ee204431c90d314c1cc08d28d23e5b7927cc",
        filePrefix = "base",
        encoderBytes = 29_120_534,
        encoderSha256 = "0b8fb1304b6109976038efff5ace81720e00386f3ff6b54ee8c75291ca0a1e11",
        decoderBytes = 130_672_026,
        decoderSha256 = "9759d217388a01b3a4c7c15533201067b48ae819c4daafc8624e64b9409dc02d",
        approximateRamBytes = 850_000_000,
    )
}
