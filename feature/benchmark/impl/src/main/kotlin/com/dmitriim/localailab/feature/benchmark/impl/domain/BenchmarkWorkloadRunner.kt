package com.dmitriim.localailab.feature.benchmark.impl.domain

import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkStartupMode
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkWorkload

interface BenchmarkWorkloadRunner {
    suspend fun run(
        workload: BenchmarkWorkload,
        runId: String,
        iteration: Int,
        startupMode: BenchmarkStartupMode,
    ): BenchmarkWorkloadResult

    fun unload(workload: BenchmarkWorkload)
}
