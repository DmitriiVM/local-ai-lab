package com.dmitriim.localailab.ai.api.memory

/** Connects one feature's visible lifecycle to its runtime-memory lease. */
class FeatureRuntimeLifecycle(
    private val memoryManager: AiRuntimeMemoryManager,
    private val runtimeKinds: Set<AiRuntimeKind>,
    private val onRelease: () -> Unit,
) {
    private val lock = Any()
    private var lease: AutoCloseable? = null

    fun onVisible() {
        synchronized(lock) {
            if (lease == null) lease = memoryManager.acquire(runtimeKinds)
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
