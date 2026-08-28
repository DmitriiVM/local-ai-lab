package com.dmitriim.localailab.ai.llamacpp.catalog

import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.llamacpp.LlamaCppRuntimeProfile
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class Phi4MiniInstructQ4KmModelDefinition(
    override val runtimeProfile: LlamaCppRuntimeProfile,
) : ModelCatalogContribution {
    override val catalogModel = llamaCppCatalogModel(
        profileKey = runtimeProfile.key,
        modelId = "phi-4-mini-instruct-q4-k-m",
        displayName = "Phi-4 Mini Instruct Q4_K_M",
        family = "Phi-4 Mini",
        description = "A multilingual instruction-tuned chat model packaged as a Q4_K_M GGUF.",
        repository = "unsloth/Phi-4-mini-instruct-GGUF",
        revision = "78eb92a46fc37e6b524df991ed9aca9bc6aa7b80",
        fileName = "Phi-4-mini-instruct-Q4_K_M.gguf",
        quantization = "Q4_K_M",
        expectedBytes = 2_491_874_272,
        sha256 = "88c00229914083cd112853aab84ed51b87bdf6b9ce42f532d8c85c7c63b1730a",
        languages = linkedSetOf("English", "Chinese", "French", "German", "Russian", "Spanish"),
        supportedLanguageCount = 23,
        approximateRamBytes = 4_000_000_000,
        licenseName = "MIT",
        attribution = "Phi-4 Mini by Microsoft; quantized GGUF by Unsloth. MIT licensed.",
    )
}
