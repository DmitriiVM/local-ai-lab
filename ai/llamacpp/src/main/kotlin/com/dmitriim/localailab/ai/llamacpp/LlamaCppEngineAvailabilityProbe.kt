package com.dmitriim.localailab.ai.llamacpp

import android.os.Build
import com.dmitriim.localailab.ai.api.availability.EngineAvailabilityProbe
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.engine.ComputePreference
import com.dmitriim.localailab.core.model.engine.EngineAvailability
import com.dmitriim.localailab.core.model.engine.EngineDescriptor
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.engine.EngineKind
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Inject
@ContributesIntoSet(AppScope::class)
class LlamaCppEngineAvailabilityProbe : EngineAvailabilityProbe {
    override suspend fun probe(): EngineAvailability = withContext(Dispatchers.IO) {
        if ("arm64-v8a" !in Build.SUPPORTED_ABIS) {
            return@withContext EngineAvailability.Unsupported(
                descriptor = descriptor,
                reason = "The bundled llama.cpp runtime requires an arm64-v8a device.",
            )
        }

        runCatching { System.loadLibrary("local_ai_llamacpp") }.fold(
            onSuccess = {
                EngineAvailability.Available(
                    descriptor = descriptor,
                    effectiveComputePreference = ComputePreference.CPU,
                    detail = "Bundled arm64 CPU runtime is available.",
                    requestedComputePreference = ComputePreference.CPU,
                    computeDetail = "ggml CPU",
                    effectiveThreadCount = Runtime.getRuntime().availableProcessors(),
                )
            },
            onFailure = { error ->
                EngineAvailability.TemporarilyUnavailable(
                    descriptor = descriptor,
                    reason = error.message ?: "The llama.cpp native runtime could not be loaded.",
                )
            },
        )
    }

    private companion object {
        val descriptor = EngineDescriptor(
            id = EngineId("llama.cpp"),
            displayName = "llama.cpp",
            kind = EngineKind.CUSTOM,
            capabilities = setOf(AiCapability.CHAT),
            bundledRuntime = true,
        )
    }
}
