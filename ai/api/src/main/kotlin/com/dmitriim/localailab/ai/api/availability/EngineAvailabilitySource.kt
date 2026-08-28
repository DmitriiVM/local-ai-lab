package com.dmitriim.localailab.ai.api.availability

import com.dmitriim.localailab.core.model.engine.EngineAvailability
import kotlinx.coroutines.flow.StateFlow

/**
 * Exposes a refreshable snapshot of availability for every packaged inference engine.
 *
 * [availability] contains the last completed refresh and may initially be empty. A failed
 * [refresh] leaves the previous snapshot intact unless an implementation documents otherwise.
 */
interface EngineAvailabilitySource {
    val availability: StateFlow<List<EngineAvailability>>

    /** Refreshes the availability snapshot. This work may inspect device capabilities. */
    suspend fun refresh()
}
