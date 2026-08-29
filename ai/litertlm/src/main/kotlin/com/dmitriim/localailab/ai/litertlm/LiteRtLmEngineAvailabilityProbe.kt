package com.dmitriim.localailab.ai.litertlm

import android.os.Build
import com.dmitriim.localailab.ai.api.availability.EngineAvailabilityProbe
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.engine.ComputePreference
import com.dmitriim.localailab.ai.api.engine.EngineAvailability
import com.dmitriim.localailab.ai.api.engine.EngineDescriptor
import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.engine.EngineKind
import com.dmitriim.localailab.ai.api.engine.NativeAbiSupport
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Inject
@ContributesIntoSet(AppScope::class)
class LiteRtLmEngineAvailabilityProbe : EngineAvailabilityProbe {
    override suspend fun probe(): EngineAvailability = withContext(Dispatchers.IO) {
        if (!NativeAbiSupport.supports(Build.SUPPORTED_ABIS.toList())) {
            return@withContext EngineAvailability.Unsupported(
                descriptor = descriptor,
                reason = "The packaged LiteRT-LM Android runtime requires arm64-v8a or x86_64.",
            )
        }
        runCatching {
            LiteRtLmNativeLibrary.load()
            Class.forName("com.google.ai.edge.litertlm.Engine")
        }.fold(
            onSuccess = {
                EngineAvailability.Available(
                    descriptor = descriptor,
                    effectiveComputePreference = ComputePreference.CPU,
                    requestedComputePreference = ComputePreference.AUTO,
                    detail = "Packaged LiteRT-LM CPU and GPU runtime is available.",
                    computeDetail = "LiteRT-LM CPU (automatic default)",
                    fallbackReason = "GPU can be selected per chat model in Assistant settings.",
                )
            },
            onFailure = { error ->
                EngineAvailability.TemporarilyUnavailable(
                    descriptor = descriptor,
                    reason = error.message ?: "The LiteRT-LM runtime could not be loaded.",
                )
            },
        )
    }

    private companion object {
        val descriptor = EngineDescriptor(
            id = EngineId("litert-lm"),
            displayName = "LiteRT-LM",
            kind = EngineKind.CUSTOM,
            capabilities = setOf(AiCapability.CHAT),
            bundledRuntime = true,
        )
    }
}
