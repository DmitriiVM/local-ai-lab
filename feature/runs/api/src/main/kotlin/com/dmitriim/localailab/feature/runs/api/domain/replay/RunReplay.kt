package com.dmitriim.localailab.feature.runs.api.domain.replay

import com.dmitriim.localailab.feature.runs.api.domain.history.RunRecord
import kotlinx.coroutines.flow.StateFlow

/** Provides the short-lived handoff from selected run history to a capability workflow. */
interface RunReplay {
    val pending: StateFlow<RunRecord?>

    fun select(run: RunRecord)

    fun consume(id: String)
}
