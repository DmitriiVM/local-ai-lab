package com.dmitriim.localaiplayground.ai.api.availability

import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.engine.EngineAvailability
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultEngineAvailabilitySource(
    private val probes: Set<EngineAvailabilityProbe>,
) : EngineAvailabilitySource {
    private val refreshMutex = Mutex()
    private val mutableAvailability = MutableStateFlow<List<EngineAvailability>>(emptyList())

    override val availability: StateFlow<List<EngineAvailability>> =
        mutableAvailability.asStateFlow()

    override suspend fun refresh() {
        refreshMutex.withLock {
            mutableAvailability.value = coroutineScope {
                probes
                    .map { probe -> async { probe.probe() } }
                    .awaitAll()
                    .sortedBy { result -> result.descriptor.displayName }
            }
        }
    }
}
