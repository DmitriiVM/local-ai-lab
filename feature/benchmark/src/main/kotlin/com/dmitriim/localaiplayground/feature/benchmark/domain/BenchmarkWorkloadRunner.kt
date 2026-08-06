package com.dmitriim.localaiplayground.feature.benchmark.domain

import com.dmitriim.localaiplayground.core.performance.BenchmarkStartupMode
import com.dmitriim.localaiplayground.core.performance.BenchmarkWorkload

interface BenchmarkWorkloadRunner {
    suspend fun run(
        workload: BenchmarkWorkload,
        runId: String,
        iteration: Int,
        startupMode: BenchmarkStartupMode,
    ): BenchmarkWorkloadResult

    fun unload(workload: BenchmarkWorkload)
}
