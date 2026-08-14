package com.dmitriim.localailab.ai.api.availability

import com.dmitriim.localailab.core.model.engine.EngineAvailability
import kotlinx.coroutines.flow.StateFlow

interface EngineAvailabilitySource {
    val availability: StateFlow<List<EngineAvailability>>

    suspend fun refresh()
}
