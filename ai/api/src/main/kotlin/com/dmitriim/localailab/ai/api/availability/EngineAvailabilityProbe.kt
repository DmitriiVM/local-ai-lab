package com.dmitriim.localailab.ai.api.availability

import com.dmitriim.localailab.core.model.engine.EngineAvailability

/** Performs one runtime-specific availability check for the current device. */
interface EngineAvailabilityProbe {
    suspend fun probe(): EngineAvailability
}
