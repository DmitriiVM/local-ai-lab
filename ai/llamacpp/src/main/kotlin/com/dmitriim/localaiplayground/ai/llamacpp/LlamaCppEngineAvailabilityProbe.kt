package com.dmitriim.localaiplayground.ai.llamacpp

import android.os.Build
import com.dmitriim.localaiplayground.ai.api.EngineAvailabilityProbe
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.AiCapability
import com.dmitriim.localaiplayground.core.model.EngineAvailability
import com.dmitriim.localaiplayground.core.model.EngineDescriptor
import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.core.model.EngineKind
import com.dmitriim.localaiplayground.core.model.RuntimeBackend
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
                    effectiveBackend = RuntimeBackend.CPU,
                    detail = "Bundled arm64 CPU runtime is available.",
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
