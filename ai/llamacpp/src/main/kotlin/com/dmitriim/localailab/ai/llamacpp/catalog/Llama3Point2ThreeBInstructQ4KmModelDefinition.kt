package com.dmitriim.localailab.ai.llamacpp.catalog

import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.llamacpp.LlamaCppRuntimeProfile
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class Llama3Point2ThreeBInstructQ4KmModelDefinition(
    override val runtimeProfile: LlamaCppRuntimeProfile,
) : ModelCatalogContribution {
    override val catalogModel = llamaCppCatalogModel(
        modelId = "llama-3.2-3b-instruct-q4-k-m",
        displayName = "Llama 3.2 3B Instruct Q4_K_M",
        family = "Llama 3.2",
        description = "A 3B instruction-tuned chat model covering eight catalogued languages in Q4_K_M format.",
        repository = "bartowski/Llama-3.2-3B-Instruct-GGUF",
        revision = "5ab33fa94d1d04e903623ae72c95d1696f09f9e8",
        fileName = "Llama-3.2-3B-Instruct-Q4_K_M.gguf",
        quantization = "Q4_K_M",
        expectedBytes = 2_019_377_696,
        sha256 = "6c1a2b41161032677be168d354123594c0e6e67d2b9227c84f296ad037c728ff",
        languages = linkedSetOf("English", "German", "French", "Italian", "Portuguese", "Hindi", "Spanish", "Thai"),
        supportedLanguageCount = 8,
        approximateRamBytes = 3_400_000_000,
        licenseName = "Llama 3.2 Community License",
        attribution = "Llama 3.2 by Meta; quantized GGUF by bartowski. The Llama 3.2 Community License applies.",
    )
}
