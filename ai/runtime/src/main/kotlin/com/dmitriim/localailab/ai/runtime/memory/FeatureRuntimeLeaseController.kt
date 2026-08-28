package com.dmitriim.localailab.ai.runtime.memory

import com.dmitriim.localailab.ai.api.memory.AiRuntimeKind
import com.dmitriim.localailab.ai.api.memory.AiRuntimeLeaseManager

/** Connects one feature's visible lifecycle to its runtime-memory lease. */
class FeatureRuntimeLeaseController(
    private val leaseManager: AiRuntimeLeaseManager,
    private val runtimeKinds: Set<AiRuntimeKind>,
    private val onRelease: () -> Unit,
) {
    private val lock = Any()
    private var lease: AutoCloseable? = null

    fun onVisible() {
        synchronized(lock) {
            if (lease == null) lease = leaseManager.acquire(runtimeKinds)
        }
    }

    fun onHidden() {
        val releasedLease = synchronized(lock) {
            lease?.also { lease = null }
        } ?: return
        try {
            onRelease()
        } finally {
            releasedLease.close()
        }
    }
}
