package com.dmitriim.localailab.ai.llamacpp.catalog

import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.library.ModelCatalogContribution
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class QwenThreePointFive0Point8bQ4ModelDefinition : ModelCatalogContribution {
    override val catalogModel = llamaCppCatalogModel(
        modelId = "qwen3.5-0.8b-q4-0",
        displayName = "Qwen3.5 0.8B Q4_0",
        family = "Qwen3.5",
        description = "A compact 0.8B English-and-Chinese chat model packaged as a Q4_0 GGUF.",
        repository = "ggml-org/Qwen3.5-0.8B-GGUF",
        revision = "8fea620810c4afa23dd6443f999a48574c1611a3",
        fileName = "Qwen3.5-0.8B-Q4_0.gguf",
        quantization = "Q4_0",
        expectedBytes = 563_036_064,
        sha256 = "57d1997790d1744fba5b40a7317df71ea5e2acee28c47e78f0cce39c0703f8cf",
        languages = linkedSetOf("English", "Chinese"),
        approximateRamBytes = 1_200_000_000,
    )
}
