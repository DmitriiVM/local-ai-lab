package com.dmitriim.localailab.ai.api.availability

import com.dmitriim.localailab.ai.api.engine.EngineAvailability

/**
 * Performs one runtime-specific availability check for the current device.
 *
 * Implementations report availability rather than loading a model. Callers may invoke [probe]
 * repeatedly; each result must describe the device state observed for that invocation.
 */
interface EngineAvailabilityProbe {
    /** Performs the potentially expensive device check away from the Android main thread. */
    suspend fun probe(): EngineAvailability
}
