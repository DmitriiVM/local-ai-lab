package com.dmitriim.localailab.ai.api.availability

import com.dmitriim.localailab.core.model.engine.EngineAvailability

interface EngineAvailabilityProbe {
    suspend fun probe(): EngineAvailability
}
