package com.dmitriim.localailab.ai.llamacpp.catalog

import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.llamacpp.LlamaCppRuntimeProfile
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class Llama3Point2OneBInstructQ4KmModelDefinition(
    override val runtimeProfile: LlamaCppRuntimeProfile,
) : ModelCatalogContribution {
    override val catalogModel = llamaCppCatalogModel(
        profileKey = runtimeProfile.key,
        modelId = "llama-3.2-1b-instruct-q4-k-m",
        displayName = "Llama 3.2 1B Instruct Q4_K_M",
        family = "Llama 3.2",
        description = "A compact 1B instruction-tuned chat model covering eight catalogued languages.",
        repository = "bartowski/Llama-3.2-1B-Instruct-GGUF",
        revision = "067b946cf014b7c697f3654f621d577a3e3afd1c",
        fileName = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
        quantization = "Q4_K_M",
        expectedBytes = 807_694_464,
        sha256 = "6f85a640a97cf2bf5b8e764087b1e83da0fdb51d7c9fab7d0fece9385611df83",
        languages = linkedSetOf("English", "German", "French", "Italian", "Portuguese", "Hindi", "Spanish", "Thai"),
        supportedLanguageCount = 8,
        approximateRamBytes = 1_500_000_000,
        licenseName = "Llama 3.2 Community License",
        attribution = "Llama 3.2 by Meta; quantized GGUF by bartowski. The Llama 3.2 Community License applies.",
    )
}
