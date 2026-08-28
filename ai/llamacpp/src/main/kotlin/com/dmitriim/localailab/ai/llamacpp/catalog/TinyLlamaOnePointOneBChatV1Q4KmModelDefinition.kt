package com.dmitriim.localailab.ai.llamacpp.catalog

import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.llamacpp.LlamaCppRuntimeProfile
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class TinyLlamaOnePointOneBChatV1Q4KmModelDefinition(
    override val runtimeProfile: LlamaCppRuntimeProfile,
) : ModelCatalogContribution {
    override val catalogModel = llamaCppCatalogModel(
        profileKey = runtimeProfile.key,
        modelId = "tinyllama-1.1b-chat-v1.0-q4-k-m",
        displayName = "TinyLlama 1.1B Chat v1.0 Q4_K_M",
        family = "TinyLlama",
        description = "A compact 1.1B English chat model packaged as a Q4_K_M GGUF.",
        repository = "TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF",
        revision = "52e7645ba7c309695bec7ac98f4f005b139cf465",
        fileName = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
        quantization = "Q4_K_M",
        expectedBytes = 668_788_096,
        sha256 = "9fecc3b3cd76bba89d504f29b616eedf7da85b96540e490ca5824d3f7d2776a0",
        languages = linkedSetOf("English"),
        supportedLanguageCount = 1,
        approximateRamBytes = 1_400_000_000,
        licenseName = "Apache-2.0",
        attribution = "TinyLlama 1.1B Chat v1.0 by TinyLlama; GGUF quantization by TheBloke.",
    )
}
