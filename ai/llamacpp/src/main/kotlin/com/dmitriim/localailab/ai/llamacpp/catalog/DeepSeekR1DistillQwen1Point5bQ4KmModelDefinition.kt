package com.dmitriim.localailab.ai.llamacpp.catalog

import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.library.ModelCatalogContribution
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class DeepSeekR1DistillQwen1Point5bQ4KmModelDefinition : ModelCatalogContribution {
    override val catalogModel = llamaCppCatalogModel(
        modelId = "deepseek-r1-distill-qwen-1.5b-q4-k-m",
        displayName = "DeepSeek R1 Distill Qwen 1.5B Q4_K_M",
        family = "DeepSeek R1 Distill Qwen",
        description = "A 1.5B distilled English-and-Chinese chat model packaged as a Q4_K_M GGUF.",
        repository = "bartowski/DeepSeek-R1-Distill-Qwen-1.5B-GGUF",
        revision = "9cc28b17e86fa2415fcb070f8ee5ec27c965aa61",
        fileName = "DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
        quantization = "Q4_K_M",
        expectedBytes = 1_117_320_800,
        sha256 = "1741e5b2d062b07acf048bf0d2c514dadf2a48f94e2b4aa0cfe069af3838ee2f",
        languages = linkedSetOf("English", "Chinese"),
        supportedLanguageCount = 2,
        approximateRamBytes = 2_100_000_000,
        licenseName = "MIT",
        attribution = "DeepSeek R1 Distill Qwen by DeepSeek; quantized GGUF by bartowski. MIT licensed.",
    )
}
