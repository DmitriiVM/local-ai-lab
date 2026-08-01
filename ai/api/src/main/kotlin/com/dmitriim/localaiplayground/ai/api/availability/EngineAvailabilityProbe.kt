package com.dmitriim.localaiplayground.ai.api.availability

import com.dmitriim.localaiplayground.core.model.engine.EngineAvailability

interface EngineAvailabilityProbe {
    suspend fun probe(): EngineAvailability
}
