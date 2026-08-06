package com.dmitriim.localaiplayground.core.performance

import com.dmitriim.localaiplayground.core.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Carries a screen-owned workload into the shared profiling workflow. */
@Inject
@SingleIn(AppScope::class)
class ProfileLaunchCoordinator {
    private val mutableWorkload = MutableStateFlow<BenchmarkWorkload?>(null)
    val workload: StateFlow<BenchmarkWorkload?> = mutableWorkload.asStateFlow()

    fun open(workload: BenchmarkWorkload) {
        mutableWorkload.value = workload
    }
}
