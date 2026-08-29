package com.dmitriim.localailab.feature.benchmark.domain

import com.dmitriim.localailab.core.performance.benchmark.BenchmarkStartupMode
import com.dmitriim.localailab.core.performance.benchmark.BenchmarkWorkload

interface BenchmarkWorkloadRunner {
    suspend fun run(
        workload: BenchmarkWorkload,
        runId: String,
        iteration: Int,
        startupMode: BenchmarkStartupMode,
    ): BenchmarkWorkloadResult

    fun unload(workload: BenchmarkWorkload)
}
