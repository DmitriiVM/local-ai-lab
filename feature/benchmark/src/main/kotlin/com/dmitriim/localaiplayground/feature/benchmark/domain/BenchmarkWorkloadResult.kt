package com.dmitriim.localaiplayground.feature.benchmark.domain

import com.dmitriim.localaiplayground.core.model.runs.RunModelSnapshot
import com.dmitriim.localaiplayground.core.performance.BenchmarkIterationResult

data class BenchmarkWorkloadResult(
    val iteration: BenchmarkIterationResult,
    val model: RunModelSnapshot,
    val input: String,
    val output: String?,
    val parametersJson: String,
    val metricsJson: String,
)
