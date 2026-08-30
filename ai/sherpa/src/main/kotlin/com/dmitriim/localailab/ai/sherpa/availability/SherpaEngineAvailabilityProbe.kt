package com.dmitriim.localailab.ai.sherpa.availability

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
class SherpaEngineAvailabilityProbe : EngineAvailabilityProbe {
    override suspend fun probe(): EngineAvailability = withContext(Dispatchers.IO) {
        if (!NativeAbiSupport.supports(Build.SUPPORTED_ABIS.toList())) {
            return@withContext EngineAvailability.Unsupported(
                descriptor = descriptor,
                reason = "The bundled sherpa-onnx runtime requires arm64-v8a or x86_64.",
            )
        }

        runCatching {
            Class.forName("com.k2fsa.sherpa.onnx.OfflineRecognizer", true, javaClass.classLoader)
        }.fold(
            onSuccess = {
                EngineAvailability.Available(
                    descriptor = descriptor,
                    effectiveComputePreference = ComputePreference.CPU,
                    detail = "Bundled CPU runtime is available.",
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
