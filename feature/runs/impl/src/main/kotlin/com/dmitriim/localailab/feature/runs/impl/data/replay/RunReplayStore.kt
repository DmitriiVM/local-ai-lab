package com.dmitriim.localailab.feature.runs.impl.data.replay

import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.feature.runs.api.domain.history.RunRecord
import com.dmitriim.localailab.feature.runs.api.domain.replay.RunReplay
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Short-lived navigation handoff; run records themselves remain the durable source of truth. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<RunReplay>())
class RunReplayStore : RunReplay {
    private val mutablePending = MutableStateFlow<RunRecord?>(null)
    override val pending: StateFlow<RunRecord?> = mutablePending

    override fun select(run: RunRecord) {
        mutablePending.value = run
    }
    override fun consume(id: String) {
        if (mutablePending.value?.id == id) mutablePending.value = null
    }
}
