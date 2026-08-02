package com.dmitriim.localaiplayground.ai.litertlm

import android.os.Build
import com.dmitriim.localaiplayground.ai.api.availability.EngineAvailabilityProbe
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.engine.ComputePreference
import com.dmitriim.localaiplayground.core.model.engine.EngineAvailability
import com.dmitriim.localaiplayground.core.model.engine.EngineDescriptor
import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.engine.EngineKind
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Inject
@ContributesIntoSet(AppScope::class)
class LiteRtLmEngineAvailabilityProbe : EngineAvailabilityProbe {
    override suspend fun probe(): EngineAvailability = withContext(Dispatchers.IO) {
        if ("arm64-v8a" !in Build.SUPPORTED_ABIS) {
            return@withContext EngineAvailability.Unsupported(
                descriptor = descriptor,
                reason = "The packaged LiteRT-LM Android runtime requires an arm64-v8a device.",
            )
        }
        runCatching { Class.forName("com.google.ai.edge.litertlm.Engine") }.fold(
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
