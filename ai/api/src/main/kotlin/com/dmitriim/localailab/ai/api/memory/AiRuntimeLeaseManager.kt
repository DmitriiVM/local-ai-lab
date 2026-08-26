package com.dmitriim.localailab.ai.api.memory

/** Identifies a runtime family whose loaded resources can be retained or evicted together. */
enum class AiRuntimeKind {
    CHAT,
    SPEECH_TO_TEXT,
    TEXT_TO_SPEECH,
}

/** Keeps runtimes warm while a feature owns them and evicts them when ownership ends. */
interface AiRuntimeLeaseManager {
    fun acquire(runtimeKinds: Set<AiRuntimeKind>): AutoCloseable

    fun evictAll()
}
