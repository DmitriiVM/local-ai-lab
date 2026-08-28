package com.dmitriim.localailab.ai.api.memory

/** Identifies a runtime family whose loaded resources can be retained or evicted together. */
enum class AiRuntimeKind {
    CHAT,
    SPEECH_TO_TEXT,
    TEXT_TO_SPEECH,
}

/**
 * Coordinates ownership of memory-heavy runtimes across visible features.
 *
 * [acquire] retains the requested runtime families until the returned lease is closed. Leases are
 * reference-counted and must always be closed. [evictAll] is an immediate best-effort request for
 * every runtime to cancel work and release memory, typically after memory pressure.
 */
interface AiRuntimeLeaseManager {
    /** Acquires a lease for non-empty runtime families. The returned lease is idempotently closeable. */
    fun acquire(runtimeKinds: Set<AiRuntimeKind>): AutoCloseable

    /** Requests immediate eviction of every runtime family, regardless of active leases. */
    fun evictAll()
}
