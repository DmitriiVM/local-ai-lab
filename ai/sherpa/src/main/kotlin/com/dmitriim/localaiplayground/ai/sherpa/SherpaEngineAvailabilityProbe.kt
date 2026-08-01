package com.dmitriim.localaiplayground.ai.sherpa

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
class SherpaEngineAvailabilityProbe : EngineAvailabilityProbe {
    override suspend fun probe(): EngineAvailability = withContext(Dispatchers.IO) {
        if ("arm64-v8a" !in Build.SUPPORTED_ABIS) {
            return@withContext EngineAvailability.Unsupported(
                descriptor = descriptor,
                reason = "The bundled sherpa-onnx runtime requires an arm64-v8a device.",
            )
        }

        runCatching {
            Class.forName("com.k2fsa.sherpa.onnx.OfflineRecognizer", true, javaClass.classLoader)
        }.fold(
            onSuccess = {
                EngineAvailability.Available(
                    descriptor = descriptor,
                    effectiveComputePreference = ComputePreference.CPU,
                    detail = "Bundled arm64 CPU runtime is available.",
                    requestedComputePreference = ComputePreference.CPU,
                    computeDetail = "ONNX Runtime CPU",
                    effectiveThreadCount = Runtime.getRuntime().availableProcessors(),
                )
            },
            onFailure = { error ->
                EngineAvailability.TemporarilyUnavailable(
                    descriptor = descriptor,
                    reason = error.message ?: "The sherpa-onnx runtime could not be loaded.",
                )
            },
        )
    }

    private companion object {
        val descriptor = EngineDescriptor(
            id = EngineId("sherpa-onnx"),
            displayName = "sherpa-onnx",
            kind = EngineKind.CUSTOM,
            capabilities = setOf(
                AiCapability.SPEECH_TO_TEXT,
                AiCapability.TEXT_TO_SPEECH,
            ),
            bundledRuntime = true,
        )
    }
}
