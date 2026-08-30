package com.dmitriim.localailab.ai.llamacpp

import android.os.Build
import com.dmitriim.localailab.ai.api.availability.EngineAvailabilityProbe
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.engine.ComputePreference
import com.dmitriim.localailab.ai.api.engine.EngineAvailability
import com.dmitriim.localailab.ai.api.engine.EngineDescriptor
import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.engine.EngineKind
import com.dmitriim.localailab.ai.api.engine.NativeAbiSupport
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Inject
@ContributesIntoSet(AppScope::class)
class LlamaCppEngineAvailabilityProbe : EngineAvailabilityProbe {
    override suspend fun probe(): EngineAvailability = withContext(Dispatchers.IO) {
        if (!NativeAbiSupport.supports(Build.SUPPORTED_ABIS.toList())) {
            return@withContext EngineAvailability.Unsupported(
                descriptor = descriptor,
                reason = "The bundled llama.cpp runtime requires arm64-v8a or x86_64.",
            )
        }

        runCatching { System.loadLibrary("local_ai_llamacpp") }.fold(
            onSuccess = {
                EngineAvailability.Available(
                    descriptor = descriptor,
                    effectiveComputePreference = ComputePreference.CPU,
                    detail = "Bundled CPU runtime is available.",
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
