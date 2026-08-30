package com.dmitriim.localailab.feature.benchmark.impl.presentation

import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.memory.AiRuntimeKind
import com.dmitriim.localailab.ai.api.memory.AiRuntimeLeaseManager
import com.dmitriim.localailab.ai.runtime.memory.FeatureRuntimeLeaseController
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkPlan
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkSessionSummary
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkStartupMode
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkWorkload
import com.dmitriim.localailab.feature.benchmark.api.launch.ProfileWorkloadStore
import com.dmitriim.localailab.feature.benchmark.impl.domain.BenchmarkWorkloadResult
import com.dmitriim.localailab.feature.benchmark.impl.domain.LocalBenchmarkWorkloadRunner
import com.dmitriim.localailab.feature.runs.api.data.RunRepository
import com.dmitriim.localailab.feature.runs.api.domain.history.RunKind
import com.dmitriim.localailab.feature.runs.api.domain.history.RunRecord
import com.dmitriim.localailab.feature.runs.api.domain.history.RunStatus
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class BenchmarkLabViewModel(
    private val profileWorkloadStore: ProfileWorkloadStore,
    private val runner: LocalBenchmarkWorkloadRunner,
    private val runRepository: RunRepository,
    runtimeLeaseManager: AiRuntimeLeaseManager,
) : ViewModel() {
    private val mutableState = MutableStateFlow(BenchmarkLabUiState(workload = profileWorkloadStore.workload.value))
    val state: StateFlow<BenchmarkLabUiState> = mutableState.asStateFlow()
    private var benchmarkJob: Job? = null
    internal val runtimeLeaseController = FeatureRuntimeLeaseController(
        leaseManager = runtimeLeaseManager,
        runtimeKinds = AiRuntimeKind.entries.toSet(),
        onRelease = ::cancel,
    )

    init {
        viewModelScope.launch {
            profileWorkloadStore.workload.collectLatest { workload ->
                mutableState.update { it.copy(workload = workload, completedIterations = emptyList(), summary = null, message = null) }
            }
        }
    }

    fun setWarmupIterations(value: Int) = mutableState.update { it.copy(warmupIterations = value.coerceIn(0, 5)) }
    fun setMeasuredIterations(value: Int) = mutableState.update { it.copy(measuredIterations = value.coerceIn(1, 25)) }
    fun toggleStartupMode() = mutableState.update {
        it.copy(startupMode = if (it.startupMode == BenchmarkStartupMode.WARM) BenchmarkStartupMode.COLD else BenchmarkStartupMode.WARM)
    }

    fun start() {
        if (benchmarkJob?.isActive == true) return
        val initial = state.value
        val workload = initial.workload ?: return showMessage("Open Profile from Chat, STT, or TTS first.")
        benchmarkJob = viewModelScope.launch(Dispatchers.Default) {
            val capability = workload.capability
            val sessionId = UUID.randomUUID().toString()
            val plan = BenchmarkPlan(
                sessionId = sessionId,
                capability = capability,
                modelId = workload.modelId.value,
                modelDisplayName = workload.modelDisplayName,
                workloadFingerprint = workload.fingerprint(initial.startupMode),
                warmupIterations = initial.warmupIterations,
                measuredIterations = initial.measuredIterations,
                startupMode = initial.startupMode,
                parametersJson = workload.parameters(initial),
            )
            val startedAt = System.currentTimeMillis()
            val results = mutableListOf<BenchmarkWorkloadResult>()
            var status = RunStatus.SUCCEEDED
            var failure: String? = null
            mutableState.update { it.copy(isRunning = true, completedIterations = emptyList(), summary = null, message = "Running warm-ups…") }
            try {
                repeat(plan.warmupIterations) { index -> runner.run(workload, UUID.randomUUID().toString(), -(index + 1), plan.startupMode) }
                for (iteration in 1..plan.measuredIterations) {
                    val iterationStartedAt = System.currentTimeMillis()
                    val result = runner.run(workload, UUID.randomUUID().toString(), iteration, plan.startupMode)
                    results += result
                    runRepository.saveRun(result.toRunRecord(sessionId, capability, iterationStartedAt))
                    mutableState.update { it.copy(completedIterations = results.map(BenchmarkWorkloadResult::iteration), message = "Measured ${results.size} of ${plan.measuredIterations}") }
                    if ((result.iteration.telemetry.resources?.thermalStatusEnd ?: Int.MIN_VALUE) >= PowerManager.THERMAL_STATUS_SEVERE) {
                        status = RunStatus.CANCELLED
                        failure = "Stopped at severe thermal status to protect the device."
                        break
                    }
                }
            } catch (_: CancellationException) {
                status = RunStatus.CANCELLED
                failure = "Profile cancelled. Completed iterations were retained."
            } catch (error: Throwable) {
                status = RunStatus.FAILED
                failure = error.message ?: "Profiling failed."
            } finally {
                runner.unload(workload)
                val summary = results.summary(failure)
                withContext(NonCancellable) {
                    runRepository.saveRun(
                        RunRecord(
                            id = sessionId,
                            kind = RunKind.BENCHMARK_SESSION,
                            capability = capability,
                            status = status,
                            startedAtEpochMs = startedAt,
                            completedAtEpochMs = System.currentTimeMillis(),
                            model = results.firstOrNull()?.model,
                            input = workload.description,
                            parametersJson = Json.encodeToString(BenchmarkPlan.serializer(), plan),
                            metricsJson = Json.encodeToString(BenchmarkSessionSummary.serializer(), summary),
                            errorMessage = failure,
                            linkedRunIds = results.map { it.iteration.runId },
                        ),
                    )
                    mutableState.update { it.copy(isRunning = false, summary = summary, message = failure ?: "Saved ${results.size} measured iterations.") }
                }
            }
        }
    }

    fun cancel() = benchmarkJob?.cancel()

    override fun onCleared() {
        runtimeLeaseController.onHidden()
        super.onCleared()
    }

    private fun showMessage(message: String) = mutableState.update { it.copy(message = message) }
}

private val BenchmarkWorkload.capability: AiCapability
    get() = when (this) {
        is BenchmarkWorkload.Chat -> AiCapability.CHAT
        is BenchmarkWorkload.SpeechToText -> AiCapability.SPEECH_TO_TEXT
        is BenchmarkWorkload.TextToSpeech -> AiCapability.TEXT_TO_SPEECH
    }

private val BenchmarkWorkload.description: String
    get() = when (this) {
        is BenchmarkWorkload.Chat -> messages.size.toString()
        is BenchmarkWorkload.SpeechToText -> listOf(input.displayName, input.durationMs).joinToString(separator = ":")
        is BenchmarkWorkload.TextToSpeech -> text
    }

private fun BenchmarkWorkload.fingerprint(mode: BenchmarkStartupMode): String = "$capability:${modelId.value}:$mode:${description.hashCode()}"
private fun BenchmarkWorkload.parameters(state: BenchmarkLabUiState): String = "{\"startupMode\":\"${state.startupMode}\",\"warmups\":${state.warmupIterations},\"measured\":${state.measuredIterations}}"

private fun BenchmarkWorkloadResult.toRunRecord(sessionId: String, capability: AiCapability, startedAt: Long) = RunRecord(
    id = iteration.runId,
    kind = RunKind.INFERENCE,
    benchmarkSessionId = sessionId,
    capability = capability,
    status = RunStatus.SUCCEEDED,
    startedAtEpochMs = startedAt,
    completedAtEpochMs = System.currentTimeMillis(),
    model = model,
    input = input,
    output = output,
    parametersJson = parametersJson,
    metricsJson = metricsJson,
)

private fun List<BenchmarkWorkloadResult>.summary(warning: String?): BenchmarkSessionSummary {
    val latencies = map { it.iteration.latencyMs }.sorted()
    val throughput = mapNotNull { it.iteration.throughputPerSecond }.sorted()
    return BenchmarkSessionSummary(
        completedIterations = size,
        medianLatencyMs = latencies.getOrNull((latencies.size - 1) / 2),
        p95LatencyMs = latencies.getOrNull(kotlin.math.ceil(latencies.size * .95).toInt() - 1),
        minimumLatencyMs = latencies.firstOrNull(),
        maximumLatencyMs = latencies.lastOrNull(),
        medianThroughputPerSecond = throughput.getOrNull((throughput.size - 1) / 2),
        totalBatteryEnergyDeltaNwh = mapNotNull { it.iteration.telemetry.resources?.batteryEnergyDeltaNwh }.sum().takeIf { it != 0L },
        warning = warning,
    )
}
