package com.dmitriim.localaiplayground.ai.api.availability

import com.dmitriim.localaiplayground.core.model.engine.EngineAvailability
import kotlinx.coroutines.flow.StateFlow

interface EngineAvailabilitySource {
    val availability: StateFlow<List<EngineAvailability>>

    suspend fun refresh()
}
