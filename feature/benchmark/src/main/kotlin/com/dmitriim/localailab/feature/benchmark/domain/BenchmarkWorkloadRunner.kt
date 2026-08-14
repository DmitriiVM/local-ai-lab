package com.dmitriim.localailab.feature.benchmark.domain

import com.dmitriim.localailab.core.performance.BenchmarkStartupMode
import com.dmitriim.localailab.core.performance.BenchmarkWorkload

interface BenchmarkWorkloadRunner {
    suspend fun run(
        workload: BenchmarkWorkload,
        runId: String,
        iteration: Int,
        startupMode: BenchmarkStartupMode,
    ): BenchmarkWorkloadResult

    fun unload(workload: BenchmarkWorkload)
}
