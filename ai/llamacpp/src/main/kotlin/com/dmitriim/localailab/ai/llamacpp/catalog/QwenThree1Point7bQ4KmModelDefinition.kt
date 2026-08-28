package com.dmitriim.localailab.ai.llamacpp.catalog

import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.ai.api.model.ModelCatalogContribution
import com.dmitriim.localailab.ai.llamacpp.LlamaCppRuntimeProfile
import com.dmitriim.localailab.core.model.library.ModelCatalogState
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ModelCatalogContribution>())
class QwenThree1Point7bQ4KmModelDefinition(
    override val runtimeProfile: LlamaCppRuntimeProfile,
) : ModelCatalogContribution {
    override val catalogModel = llamaCppCatalogModel(
        modelId = "qwen3-1.7b-q4-k-m",
        displayName = "Qwen3 1.7B Q4_K_M",
        family = "Qwen3",
        description = "A 1.7B multilingual chat model packaged as a Q4_K_M GGUF for local text generation.",
        repository = "ggml-org/Qwen3-1.7B-GGUF",
        revision = "daeb8e2d528a760970442092f6bf1e55c3b659eb",
        fileName = "Qwen3-1.7B-Q4_K_M.gguf",
        quantization = "Q4_K_M",
        expectedBytes = 1_282_439_264,
        sha256 = "d2387ca2dbfee2ffabce7120d3770dadca0b293052bc2f0e138fdc940d9bc7b5",
        languages = linkedSetOf("English", "Russian", "Chinese"),
        supportedLanguageCount = 119,
        approximateRamBytes = 2_300_000_000,
        state = ModelCatalogState.APPROVED,
    )
}
