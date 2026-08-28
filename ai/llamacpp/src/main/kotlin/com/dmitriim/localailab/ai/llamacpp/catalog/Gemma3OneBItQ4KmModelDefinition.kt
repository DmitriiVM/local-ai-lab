package com.dmitriim.localailab.ai.llamacpp.catalog

import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.llamacpp.LlamaCppRuntimeProfile
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class Gemma3OneBItQ4KmModelDefinition(
    override val runtimeProfile: LlamaCppRuntimeProfile,
) : ModelCatalogContribution {
    override val catalogModel = llamaCppCatalogModel(
        profileKey = runtimeProfile.key,
        modelId = "gemma-3-1b-it-q4-k-m",
        displayName = "Gemma 3 1B IT Q4_K_M",
        family = "Gemma 3",
        description = "A compact 1B instruction-tuned multilingual chat model packaged as a Q4_K_M GGUF.",
        repository = "ggml-org/gemma-3-1b-it-GGUF",
        revision = "f9c28bcd85737ffc5aef028638d3341d49869c27",
        fileName = "gemma-3-1b-it-Q4_K_M.gguf",
        quantization = "Q4_K_M",
        expectedBytes = 806_058_240,
        sha256 = "8ccc5cd1f1b3602548715ae25a66ed73fd5dc68a210412eea643eb20eb75a135",
        languages = linkedSetOf("English", "Russian", "Chinese"),
        supportedLanguageCount = 140,
        approximateRamBytes = 1_600_000_000,
        licenseName = "Gemma Terms of Use",
        attribution = "Gemma 3 by Google; use is subject to the Gemma Terms of Use and Prohibited Use Policy.",
    )
}
