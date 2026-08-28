package com.dmitriim.localailab.ai.llamacpp.catalog

import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.llamacpp.LlamaCppRuntimeProfile
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class Lfm2Point5InstructQ4KmModelDefinition(
    override val runtimeProfile: LlamaCppRuntimeProfile,
) : ModelCatalogContribution {
    override val catalogModel = llamaCppCatalogModel(
        modelId = "lfm2.5-1.2b-instruct-q4-k-m",
        displayName = "LFM2.5 1.2B Instruct Q4_K_M",
        family = "LFM2.5",
        description = "A 1.2B instruction-tuned chat model covering eight catalogued languages in Q4_K_M format.",
        repository = "LiquidAI/LFM2.5-1.2B-Instruct-GGUF",
        revision = "047e06635fbe71469926b35ea414537245218200",
        fileName = "LFM2.5-1.2B-Instruct-Q4_K_M.gguf",
        quantization = "Q4_K_M",
        expectedBytes = 730_895_168,
        sha256 = "b1b3de114215d9507409a662a501a631095a479a419584e8a2ded6304b19b4f5",
        languages = linkedSetOf("English", "Arabic", "Chinese", "French", "German", "Japanese", "Korean", "Spanish"),
        supportedLanguageCount = 8,
        approximateRamBytes = 1_500_000_000,
        licenseName = "LFM Open License v1.0",
        attribution = "LFM2.5 by Liquid AI; use is subject to the LFM Open License v1.0.",
    )
}
