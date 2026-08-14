package com.dmitriim.localailab.feature.benchmark.presentation

import com.dmitriim.localailab.core.performance.BenchmarkIterationResult
import com.dmitriim.localailab.core.performance.BenchmarkSessionSummary
import com.dmitriim.localailab.core.performance.BenchmarkStartupMode
import com.dmitriim.localailab.core.performance.BenchmarkWorkload

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
