package com.dmitriim.localailab.ai.llamacpp.catalog

import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.llamacpp.LlamaCppRuntimeProfile
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class SmolLm3ThreeBQ4KmModelDefinition(
    override val runtimeProfile: LlamaCppRuntimeProfile,
) : ModelCatalogContribution {
    override val catalogModel = llamaCppCatalogModel(
        profileKey = runtimeProfile.key,
        modelId = "smollm3-3b-q4-k-m",
        displayName = "SmolLM3 3B Q4_K_M",
        family = "SmolLM3",
        description = "A 3B multilingual chat model covering six catalogued languages in Q4_K_M format.",
        repository = "ggml-org/SmolLM3-3B-GGUF",
        revision = "4965cb60b150737b68a0408c36aeefb65078f894",
        fileName = "SmolLM3-Q4_K_M.gguf",
        quantization = "Q4_K_M",
        expectedBytes = 1_915_305_312,
        sha256 = "8334b850b7bd46238c16b0c550df2138f0889bf433809008cc17a8b05761863e",
        languages = linkedSetOf("English", "French", "Spanish", "German", "Italian", "Portuguese"),
        supportedLanguageCount = 6,
        approximateRamBytes = 3_200_000_000,
    )
}
