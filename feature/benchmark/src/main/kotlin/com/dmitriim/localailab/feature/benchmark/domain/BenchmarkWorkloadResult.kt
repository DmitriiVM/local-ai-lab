package com.dmitriim.localailab.feature.benchmark.domain

import com.dmitriim.localailab.feature.runs.api.domain.history.RunModelSnapshot
import com.dmitriim.localailab.core.performance.benchmark.BenchmarkIterationResult

data class BenchmarkWorkloadResult(
    val iteration: BenchmarkIterationResult,
    val model: RunModelSnapshot,
    val input: String,
    val output: String?,
    val parametersJson: String,
    val metricsJson: String,
)
