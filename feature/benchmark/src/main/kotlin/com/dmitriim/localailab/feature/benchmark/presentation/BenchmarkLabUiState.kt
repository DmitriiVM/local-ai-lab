package com.dmitriim.localailab.feature.benchmark.presentation

import com.dmitriim.localailab.core.performance.benchmark.BenchmarkIterationResult
import com.dmitriim.localailab.core.performance.benchmark.BenchmarkSessionSummary
import com.dmitriim.localailab.core.performance.benchmark.BenchmarkStartupMode
import com.dmitriim.localailab.core.performance.benchmark.BenchmarkWorkload

data class BenchmarkLabUiState(
    val workload: BenchmarkWorkload? = null,
    val warmupIterations: Int = 2,
    val measuredIterations: Int = 10,
    val startupMode: BenchmarkStartupMode = BenchmarkStartupMode.WARM,
    val isRunning: Boolean = false,
    val completedIterations: List<BenchmarkIterationResult> = emptyList(),
    val summary: BenchmarkSessionSummary? = null,
    val message: String? = null,
)
