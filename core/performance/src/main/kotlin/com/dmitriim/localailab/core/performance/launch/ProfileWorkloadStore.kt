package com.dmitriim.localailab.core.performance.launch

import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.performance.benchmark.BenchmarkWorkload
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Stores a screen-owned workload for the shared profiling workflow. */
@Inject
@SingleIn(AppScope::class)
class ProfileWorkloadStore {
    private val mutableWorkload = MutableStateFlow<BenchmarkWorkload?>(null)
    val workload: StateFlow<BenchmarkWorkload?> = mutableWorkload.asStateFlow()

    fun open(workload: BenchmarkWorkload) {
        mutableWorkload.value = workload
    }
}
