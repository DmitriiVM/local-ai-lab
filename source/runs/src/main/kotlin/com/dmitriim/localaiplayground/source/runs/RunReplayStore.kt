package com.dmitriim.localaiplayground.source.runs

import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.RunRecord
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Short-lived navigation handoff; run records themselves remain the durable source of truth. */
@Inject
@SingleIn(AppScope::class)
class RunReplayStore {
    private val mutablePending = MutableStateFlow<RunRecord?>(null)
    val pending: StateFlow<RunRecord?> = mutablePending

    fun select(run: RunRecord) { mutablePending.value = run }
    fun consume(id: String) { if (mutablePending.value?.id == id) mutablePending.value = null }
}
