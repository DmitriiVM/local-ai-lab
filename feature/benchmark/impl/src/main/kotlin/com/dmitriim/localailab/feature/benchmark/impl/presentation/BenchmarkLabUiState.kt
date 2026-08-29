package com.dmitriim.localailab.feature.benchmark.impl.presentation

import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkIterationResult
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkSessionSummary
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkStartupMode
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkWorkload

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
