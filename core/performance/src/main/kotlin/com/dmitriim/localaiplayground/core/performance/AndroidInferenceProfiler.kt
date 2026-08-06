package com.dmitriim.localaiplayground.core.performance

import androidx.tracing.Trace
import android.os.SystemClock
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.di.ApplicationCoroutineScope
import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AndroidInferenceProfiler(
    private val sampler: AndroidInferenceResourceSampler,
    @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
) : InferenceProfiler {
    override fun start(
        runId: String,
        capability: AiCapability,
        extendedTelemetry: Boolean,
    ): InferenceProfileSession = Session(
        runId = runId,
        capability = capability,
        resourceSamplingEnabled = extendedTelemetry,
        sampler = sampler,
        scope = applicationScope,
    )

    private class Session(
        private val runId: String,
        private val capability: AiCapability,
        private val resourceSamplingEnabled: Boolean,
        private val sampler: InferenceResourceSampler,
        scope: CoroutineScope,
    ) : InferenceProfileSession {
        private val outerCookie = runId.hashCode()
        private val phaseDurations = mutableListOf<InferencePhaseDuration>()
        private val start = sampler.snapshot()
        private val device = sampler.deviceSnapshot()
        private val samples = mutableListOf(start)
        private var finished: InferenceTelemetry? = null
        private val samplerJob: Job? = if (resourceSamplingEnabled) {
            scope.launch(Dispatchers.Default) {
                while (isActive) {
                    delay(SAMPLE_INTERVAL_MS)
                    samples += sampler.snapshot()
                }
            }
        } else {
            null
        }

        init {
            Trace.beginAsyncSection("LAP/${capability.name.lowercase()}/$runId", outerCookie)
        }

        override suspend fun <T> trace(phase: InferencePhase, block: suspend () -> T): T {
            if (phase == InferencePhase.TOTAL) return block()
            val cookie = outerCookie xor phase.ordinal
            val startedAt = SystemClock.elapsedRealtime()
            Trace.beginAsyncSection("LAP/${capability.name.lowercase()}/${phase.name.lowercase()}", cookie)
            return try {
                block()
            } finally {
                Trace.endAsyncSection("LAP/${capability.name.lowercase()}/${phase.name.lowercase()}", cookie)
                phaseDurations += InferencePhaseDuration(phase, SystemClock.elapsedRealtime() - startedAt)
            }
        }

        override fun finish(): InferenceTelemetry = finished ?: run {
            samplerJob?.cancel()
            val end = sampler.snapshot()
            if (resourceSamplingEnabled) samples += end
            Trace.endAsyncSection("LAP/${capability.name.lowercase()}/$runId", outerCookie)
            InferenceTelemetry(
                runId = runId,
                capability = capability,
                traceActive = Trace.isEnabled(),
                wallDurationMs = end.elapsedRealtimeMs - start.elapsedRealtimeMs,
                phaseDurations = phaseDurations.toList(),
                resources = if (resourceSamplingEnabled) resourceMetrics(start, end, samples) else null,
                device = if (resourceSamplingEnabled) device else null,
            ).also { finished = it }
        }

        private fun resourceMetrics(
            start: InferenceResourceSnapshot,
            end: InferenceResourceSnapshot,
            samples: List<InferenceResourceSnapshot>,
        ): InferenceResourceMetrics {
            val wallMs = (end.elapsedRealtimeMs - start.elapsedRealtimeMs).coerceAtLeast(1L)
            val cpuMs = (end.processCpuTimeMs - start.processCpuTimeMs).coerceAtLeast(0L)
            val cpuPercent = cpuMs * 100.0 / wallMs
            val peakCpuPercent = samples.zipWithNext().maxOfOrNull { (before, after) ->
                val intervalMs = (after.elapsedRealtimeMs - before.elapsedRealtimeMs).coerceAtLeast(1L)
                (after.processCpuTimeMs - before.processCpuTimeMs).coerceAtLeast(0L) * 100.0 / intervalMs
            }
            val currents = samples.mapNotNull(InferenceResourceSnapshot::batteryCurrentUa)
            return InferenceResourceMetrics(
                processCpuTimeMs = cpuMs,
                averageProcessCpuPercent = cpuPercent,
                peakProcessCpuPercent = peakCpuPercent,
                startPssBytes = start.pssBytes,
                endPssBytes = end.pssBytes,
                peakPssBytes = samples.mapNotNull(InferenceResourceSnapshot::pssBytes).maxOrNull(),
                availableMemoryStartBytes = start.availableMemoryBytes,
                availableMemoryEndBytes = end.availableMemoryBytes,
                batteryEnergyDeltaNwh = start.batteryEnergyNwh?.let { initial -> end.batteryEnergyNwh?.let { initial - it } },
                batteryChargeDeltaUah = start.batteryChargeUah?.let { initial -> end.batteryChargeUah?.let { initial - it } },
                averageBatteryCurrentUa = currents.takeIf(List<Int>::isNotEmpty)?.average(),
                batteryMeasurementsAvailable = start.batteryEnergyNwh != null || start.batteryChargeUah != null || currents.isNotEmpty(),
                powerSaveMode = start.powerSaveMode || end.powerSaveMode,
                thermalStatusStart = start.thermalStatus,
                thermalStatusEnd = end.thermalStatus,
                thermalHeadroomStart = start.thermalHeadroom,
                thermalHeadroomEnd = end.thermalHeadroom,
            )
        }

        private companion object {
            const val SAMPLE_INTERVAL_MS = 500L
        }
    }
}
