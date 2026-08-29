package com.dmitriim.localailab.feature.benchmark.api.launch

import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkWorkload
import kotlinx.coroutines.flow.StateFlow

/** Shares a pending workload with the benchmark workflow. */
interface ProfileWorkloadStore {
    val workload: StateFlow<BenchmarkWorkload?>

    fun open(workload: BenchmarkWorkload)
}
