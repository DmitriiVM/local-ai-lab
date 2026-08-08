package com.dmitriim.localaiplayground.feature.benchmark.presentation

import com.dmitriim.localaiplayground.core.performance.BenchmarkIterationResult
import com.dmitriim.localaiplayground.core.performance.BenchmarkSessionSummary
import com.dmitriim.localaiplayground.core.performance.BenchmarkStartupMode
import com.dmitriim.localaiplayground.core.performance.BenchmarkWorkload

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
