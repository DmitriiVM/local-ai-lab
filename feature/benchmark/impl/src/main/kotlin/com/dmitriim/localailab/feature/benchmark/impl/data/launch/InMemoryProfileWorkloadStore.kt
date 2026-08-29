package com.dmitriim.localailab.feature.benchmark.impl.data.launch

import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.feature.benchmark.api.domain.BenchmarkWorkload
import com.dmitriim.localailab.feature.benchmark.api.launch.ProfileWorkloadStore
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ProfileWorkloadStore>())
class InMemoryProfileWorkloadStore : ProfileWorkloadStore {
    private val mutableWorkload = MutableStateFlow<BenchmarkWorkload?>(null)

    override val workload: StateFlow<BenchmarkWorkload?> = mutableWorkload.asStateFlow()

    override fun open(workload: BenchmarkWorkload) {
        mutableWorkload.value = workload
    }
}
